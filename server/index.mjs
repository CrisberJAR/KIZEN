import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createHash, randomUUID } from "node:crypto";

import { alexaStatus, armPendingNudges, chimeNudge, clientSnapshot, rememberAlexa, silenceDoneNudges } from "./alexaReminders.mjs";

const ROOT = path.dirname(fileURLToPath(import.meta.url));
loadDotEnv(path.join(ROOT, ".env"));
const DATA = process.env.KIZEN_DATA_DIR || path.join(ROOT, "data");
const PORT = Number(process.env.PORT || 8787);
const GEMINI = process.env.GEMINI_API_KEY || "";
const HOME_USER = (process.env.KIZEN_HOME_USER || "kizen-casa").trim() || "kizen-casa";

function loadDotEnv(file) {
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const split = trimmed.indexOf("=");
    if (split < 1) continue;
    const key = trimmed.slice(0, split).trim();
    const value = trimmed.slice(split + 1).trim().replace(/^["']|["']$/g, "");
    if (key && process.env[key] == null) process.env[key] = value;
  }
}

fs.mkdirSync(DATA, { recursive: true });

const TZ = process.env.KIZEN_TZ || "America/Lima";
const empty = () => ({
  lists: [],
  tasks: [],
  habits: [],
  habit_logs: [],
  day_nudges: [],
  deleted_tasks: [],
  deleted_habits: [],
  deleted_day_nudges: [],
  deleted_lists: [],
  alexa: null,
});

function looksLikeAlexaAccount(id) {
  const value = String(id || "").trim();
  return !value || value === "anon" || value.startsWith("amzn1.") || value.length > 64;
}

function resolveUser(id) {
  return looksLikeAlexaAccount(id) ? HOME_USER : String(id).trim();
}

function fileFor(userId) {
  const hash = createHash("sha256").update(String(userId || "anon")).digest("hex").slice(0, 16);
  return path.join(DATA, `${hash}.json`);
}

const userLocks = new Map();

function withUserLock(userId, job) {
  const prev = userLocks.get(userId) || Promise.resolve();
  const next = prev.then(job, job);
  userLocks.set(userId, next.catch(() => {}));
  return next;
}

function load(userId) {
  const file = fileFor(userId);
  if (!fs.existsSync(file)) return empty();
  try {
    const data = { ...empty(), ...JSON.parse(fs.readFileSync(file, "utf8")) };
    data.lists = Array.isArray(data.lists) ? data.lists : [];
    data.tasks = Array.isArray(data.tasks) ? data.tasks : [];
    data.habits = Array.isArray(data.habits) ? data.habits : [];
    data.habit_logs = Array.isArray(data.habit_logs) ? data.habit_logs : [];
    const rawNudges = Array.isArray(data.day_nudges) ? data.day_nudges : [];
    data.day_nudges = pruneDayNudges(rawNudges);
    data.deleted_tasks = Array.isArray(data.deleted_tasks) ? data.deleted_tasks : [];
    data.deleted_habits = Array.isArray(data.deleted_habits) ? data.deleted_habits : [];
    data.deleted_day_nudges = Array.isArray(data.deleted_day_nudges) ? data.deleted_day_nudges : [];
    data.deleted_lists = Array.isArray(data.deleted_lists) ? data.deleted_lists : [];
    if (JSON.stringify(data.day_nudges) !== JSON.stringify(rawNudges)) save(userId, data);
    if (!data.alexa || typeof data.alexa !== "object") data.alexa = null;
    return data;
  } catch {
    return empty();
  }
}

function save(userId, data) {
  fs.writeFileSync(fileFor(userId), JSON.stringify(data, null, 2));
}

function wins(remote, local) {
  return !local || Number(remote?.updated_at || 0) >= Number(local?.updated_at || 0);
}

function mergeById(localItems, remoteItems) {
  const map = new Map((localItems || []).map((item) => [item.id, item]));
  for (const item of remoteItems || []) {
    if (wins(item, map.get(item.id))) map.set(item.id, item);
  }
  return [...map.values()];
}

function mergeDeleted(localItems, remoteItems) {
  const map = new Map();
  for (const item of [...(localItems || []), ...(remoteItems || [])]) {
    if (!item || !item.id) continue;
    const at = Number(item.deleted_at ?? item.deletedAt ?? 0);
    const prev = map.get(item.id);
    if (prev == null || at >= prev) map.set(item.id, at);
  }
  const cutoff = Date.now() - 30 * 86_400_000;
  return [...map.entries()]
    .filter((entry) => entry[1] >= cutoff)
    .map(([id, deleted_at]) => ({ id, deleted_at }));
}

function withoutDeleted(items, deleted) {
  const ids = new Set((deleted || []).map((item) => item.id));
  return (items || []).filter((item) => !ids.has(item.id));
}

function logsWithoutDeletedHabits(logs, deletedHabits) {
  const ids = new Set((deletedHabits || []).map((item) => item.id));
  return (logs || []).filter((item) => !ids.has(item.habit_id));
}

function mergeLogs(localItems, remoteItems) {
  const map = new Map();
  for (const item of [...(localItems || []), ...(remoteItems || [])]) {
    if (!item || !item.habit_id) continue;
    const key = `${item.habit_id}:${item.day_epoch}`;
    const prev = map.get(key);
    if (!prev || Number(item.completed_at || 0) >= Number(prev.completed_at || 0)) {
      map.set(key, item);
    }
  }
  return [...map.values()];
}

function isoDow() {
  const weekday = new Intl.DateTimeFormat("en-US", { timeZone: TZ, weekday: "long" }).format(new Date());
  return weekday.toUpperCase();
}

function dayEpoch() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const num = (type) => Number(parts.find((part) => part.type === type)?.value || 0);
  return Math.floor(Date.UTC(num("year"), num("month") - 1, num("day")) / 86_400_000);
}

function normalizeNudgeDay(item) {
  if (!item || typeof item !== "object") return item;
  const today = dayEpoch();
  const epoch = Number(item.day_epoch);
  if (Number.isFinite(epoch) && Math.abs(epoch - today) <= 1) {
    return { ...item, day_epoch: today };
  }
  return item;
}

function pruneDayNudges(items) {
  const today = dayEpoch();
  return (items || [])
    .map(normalizeNudgeDay)
    .filter((item) => Number(item.day_epoch) >= today);
}

function todayNudges(user) {
  const epoch = dayEpoch();
  return (user.day_nudges || []).filter((item) => Number(item.day_epoch) === epoch);
}

function pendingNudges(user) {
  return todayNudges(user).filter((item) => !item.is_done);
}

function phrase(done, total, streak, open, nudges) {
  const aviso = nudges > 0 ? ` ${nudges} aviso${nudges === 1 ? "" : "s"} de hoy.` : "";
  if (total === 0 && open === 0 && nudges === 0) return "Hoy el día está en blanco. Un hábito pequeño basta para empezar.";
  if (done === total && total > 0 && open === 0 && nudges === 0) {
    return `Todo lo de hoy está listo. Tu mejor racha: ${streak} días. Respira.`;
  }
  if (streak >= 3) return `Racha de ${streak} días. ${done} de ${total} hábitos y ${open} tareas suaves.${aviso}`;
  return `${done} de ${total} hábitos de hoy. Sin prisa: ${open} tareas te esperan.${aviso}`;
}

function insights(user) {
  const dow = isoDow();
  const epoch = dayEpoch();
  const todayHabits = (user.habits || []).filter(
    (habit) => habit.is_active !== false && (habit.repeat_days || []).includes(dow),
  );
  const doneIds = new Set(
    (user.habit_logs || []).filter((log) => Number(log.day_epoch) === epoch).map((log) => log.habit_id),
  );
  const done = todayHabits.filter((habit) => doneIds.has(habit.id)).length;
  const streak = Math.max(0, ...todayHabits.map((habit) => Number(habit.longest_streak || habit.current_streak || 0)), 0);
  const open = (user.tasks || []).filter((task) => !task.is_done).length;
  const nudges = pendingNudges(user).length;
  return {
    text: phrase(done, todayHabits.length, streak, open, nudges),
    habits_done_today: done,
    habits_total_today: todayHabits.length,
    best_streak: streak,
    open_tasks: open,
    open_nudges: nudges,
  };
}

async function withGemini(base, user) {
  if (!GEMINI) return base;
  const openTitles = (user.tasks || []).filter((task) => !task.is_done).slice(0, 8).map((task) => task.title).join(", ");
  const doneTitles = (user.tasks || []).filter((task) => task.is_done).slice(-8).map((task) => task.title).join(", ");
  const nudgeTitles = pendingNudges(user).slice(0, 6).map((item) => item.title).join(", ");
  const prompt = `Eres Jarvis, voz suave de Kizen, para un parlante Alexa. En 1 o 2 frases cortas en español, cálidas y motivadoras, resume el día. Hecho: ${doneTitles || "aún nada"}. Pendiente: ${openTitles || "nada"}. Avisos de hoy: ${nudgeTitles || "ninguno"}. Hábitos: ${base.habits_done_today} de ${base.habits_total_today}. Racha: ${base.best_streak}. No digas que eres una IA. Sin asteriscos ni listas.`;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 4500);
  try {
    const res = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${encodeURIComponent(GEMINI)}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
        signal: controller.signal,
      },
    );
    const json = await res.json();
    const text = json?.candidates?.[0]?.content?.parts?.[0]?.text?.trim();
    if (text) return { ...base, text: text.replace(/\*/g, "").slice(0, 700) };
  } catch {
    /* keep local copy */
  } finally {
    clearTimeout(timer);
  }
  return base;
}

function shouldListNudges(intent, title, utterance) {
  if (intent === "LIST_NUDGES") return true;
  const text = `${title || ""} ${utterance || ""}`.toLowerCase();
  if (text.indexOf("aviso") === -1) return false;
  if (intent === "LIST_TASKS" || intent === "INSIGHTS") return true;
  if (intent === "ADD_TASK") {
    return /(cual|cuáles|lista|qu[eé]|mis avisos|avisos de hoy)/.test(text);
  }
  return false;
}

function defaultList(user) {
  let list = user.lists[0];
  if (!list) {
    const now = Date.now();
    list = { id: randomUUID(), name: "Alexa", color_hex: "#C7CEEA", emoji: "☁️", updated_at: now };
    user.lists.push(list);
  }
  return list;
}

async function alexa(user, body) {
  const linked = rememberAlexa(user, body);
  let intent = body.intent || "INSIGHTS";
  const title = body.task?.title || body.utterance || "";
  if (shouldListNudges(intent, title, body.utterance)) intent = "LIST_NUDGES";
  user.tasks = withoutDeleted(user.tasks, user.deleted_tasks);
  user.habits = withoutDeleted(user.habits, user.deleted_habits);
  user.day_nudges = withoutDeleted(user.day_nudges, user.deleted_day_nudges);
  const now = Date.now();
  const userId = body.user_id;
  if (linked) save(userId, user);
  if (intent === "LINK_ALEXA") {
    await armPendingNudges(user, userId, save).catch(() => {});
    return { speak: "Este Echo ya está enlazado con Kizen.", intent };
  }
  if (intent === "ADD_TASK" && title) {
    const list = defaultList(user);
    user.tasks.push({
      id: body.task?.id || randomUUID(),
      list_id: list.id,
      title,
      notes: body.task?.notes || "",
      priority: "MEDIUM",
      is_done: false,
      due_at: null,
      reminder_at: null,
      completed_at: null,
      created_at: now,
      updated_at: now,
      source: "ALEXA",
      subtasks: [],
    });
    save(userId, user);
    return { speak: `Listo. Apunté ${title} en Kizen.`, intent };
  }
  if (intent === "COMPLETE_TASK") {
    const task =
      user.tasks.find((item) => !item.is_done && title && item.title.toLowerCase().includes(title.toLowerCase())) ||
      user.tasks.find((item) => item.id === body.task_id);
    if (!task) return { speak: "No encontré esa tarea. ¿Me la dices otra vez?", intent };
    task.is_done = true;
    task.completed_at = now;
    task.updated_at = now;
    save(userId, user);
    return { speak: `Marqué ${task.title} como hecha. Qué bien.`, intent };
  }
  if (intent === "LIST_TASKS") {
    const open = user.tasks.filter((item) => !item.is_done).map((item) => item.title);
    return {
      speak: open.length ? `Tareas pendientes: ${open.slice(0, 5).join(", ")}.` : "No tienes tareas pendientes. Respira.",
      intent,
    };
  }
  if (intent === "ADD_HABIT" && title) {
    user.habits.push({
      id: randomUUID(),
      title,
      notes: "",
      emoji: "🌱",
      color_hex: "#B5EAD7",
      repeat_days: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
      reminder_minutes: 540,
      is_active: true,
      current_streak: 0,
      longest_streak: 0,
      created_at: now,
      updated_at: now,
    });
    save(userId, user);
    return { speak: `Empezamos el hábito ${title}. Despacio.`, intent };
  }
  if (intent === "COMPLETE_HABIT") {
    const habit = user.habits.find((item) => title && item.title.toLowerCase().includes(title.toLowerCase()));
    if (!habit) return { speak: "No encontré ese hábito.", intent };
    const epoch = dayEpoch();
    const goal = Math.max(1, Number(habit.times_per_day || 1));
    if (!Array.isArray(user.habit_logs)) user.habit_logs = [];
    let log = user.habit_logs.find((item) => item.habit_id === habit.id && Number(item.day_epoch) === epoch);
    if (!log) {
      log = { id: randomUUID(), habit_id: habit.id, day_epoch: epoch, count: 0, completed_at: now };
      user.habit_logs.push(log);
    }
    log.count = Math.min(goal, Number(log.count || 0) + 1);
    log.completed_at = now;
    habit.current_streak = Number(habit.current_streak || 0) + 1;
    habit.longest_streak = Math.max(Number(habit.longest_streak || 0), habit.current_streak);
    habit.updated_at = now;
    save(userId, user);
    return { speak: `Hoy sí. ${habit.title}. Llevas ${habit.current_streak} días.`, intent };
  }
  if (intent === "LIST_HABITS") {
    const names = user.habits.filter((item) => item.is_active !== false).map((item) => item.title);
    return { speak: names.length ? `Tus hábitos: ${names.join(", ")}.` : "Aún no hay hábitos.", intent };
  }
  if (intent === "ADD_NUDGE" && title) {
    if (!Array.isArray(user.day_nudges)) user.day_nudges = [];
    const nudgeId = body.task?.id || randomUUID();
    user.day_nudges.push({
      id: nudgeId,
      title,
      notes: "",
      start_at: now,
      interval_minutes: 20,
      is_done: false,
      day_epoch: dayEpoch(),
      created_at: now,
      updated_at: now,
      items: [],
    });
    save(userId, user);
    await chimeNudge(user, { id: nudgeId, title }, userId, save).catch(() => {});
    return { speak: `Aviso de hoy: ${title}. Te lo voy a repetir hasta que lo marques.`, intent };
  }
  if (intent === "COMPLETE_NUDGE") {
    const nudge =
      pendingNudges(user).find((item) => title && item.title.toLowerCase().includes(title.toLowerCase())) ||
      (user.day_nudges || []).find((item) => item.id === body.task_id);
    if (!nudge) return { speak: "No encontré ese aviso de hoy. ¿Me dices el nombre otra vez?", intent };
    nudge.is_done = true;
    nudge.updated_at = now;
    save(userId, user);
    await chimeNudge(user, { id: nudge.id, title: nudge.title, cancel: true }, userId, save).catch(() => {});
    return { speak: `Listo. Ya no te aviso de ${nudge.title}.`, intent };
  }
  if (intent === "LIST_NUDGES") {
    const open = pendingNudges(user).map((item) => item.title);
    return {
      speak: open.length ? `Avisos de hoy: ${open.slice(0, 6).join(", ")}.` : "No tienes avisos pendientes hoy.",
      intent,
    };
  }
  if (intent === "STREAK") {
    const best = user.habits.reduce((max, habit) => Math.max(max, Number(habit.longest_streak || 0)), 0);
    return { speak: best ? `Tu mejor racha es de ${best} días.` : "La racha empieza con un solo día.", intent };
  }
  const enriched = await withGemini(insights(user), user);
  return { speak: enriched.text, intent: "INSIGHTS" };
}

function json(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, X-Kizen-User-Id",
    "Access-Control-Allow-Methods": "GET,PUT,POST,OPTIONS",
  });
  res.end(status === 204 ? "" : JSON.stringify(body));
}

function bodyOf(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", (chunk) => chunks.push(chunk));
    req.on("end", () => {
      const raw = Buffer.concat(chunks).toString("utf8");
      if (!raw) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch (error) {
        reject(error);
      }
    });
    req.on("error", reject);
  });
}

http
  .createServer(async (req, res) => {
    if (req.method === "OPTIONS") return json(res, 204, {});
    const url = new URL(req.url || "/", `http://127.0.0.1:${PORT}`);
    const userId = resolveUser(req.headers["x-kizen-user-id"] || url.searchParams.get("user_id") || HOME_USER);
    console.log(`${new Date().toISOString()} ${req.method} ${url.pathname}`);
    try {
      if (req.method === "GET" && url.pathname === "/health") {
        return json(res, 200, { ok: true, gemini: Boolean(GEMINI), alexa: alexaStatus(load(HOME_USER)) });
      }
      if (req.method === "GET" && url.pathname === "/api/v3/sync") {
        return json(res, 200, clientSnapshot(load(userId)));
      }
      if (req.method === "PUT" && url.pathname === "/api/v3/sync") {
        const incoming = await bodyOf(req);
        return withUserLock(userId, async () => {
          const local = load(userId);
          const deleted_tasks = mergeDeleted(local.deleted_tasks, incoming.deleted_tasks);
          const deleted_habits = mergeDeleted(local.deleted_habits, incoming.deleted_habits);
          const deleted_day_nudges = mergeDeleted(local.deleted_day_nudges, incoming.deleted_day_nudges);
          const deleted_lists = mergeDeleted(local.deleted_lists, incoming.deleted_lists);
          const merged = {
            lists: withoutDeleted(mergeById(local.lists, incoming.lists), deleted_lists),
            tasks: withoutDeleted(mergeById(local.tasks, incoming.tasks), deleted_tasks),
            habits: withoutDeleted(mergeById(local.habits, incoming.habits), deleted_habits),
            habit_logs: logsWithoutDeletedHabits(mergeLogs(local.habit_logs, incoming.habit_logs), deleted_habits),
            day_nudges: pruneDayNudges(withoutDeleted(mergeById(local.day_nudges, incoming.day_nudges), deleted_day_nudges)),
            deleted_tasks,
            deleted_habits,
            deleted_day_nudges,
            deleted_lists,
            alexa: local.alexa || null,
          };
          save(userId, merged);
          await silenceDoneNudges(merged, userId, save).catch(() => {});
          return json(res, 200, clientSnapshot(merged));
        });
      }
      if (req.method === "GET" && url.pathname === "/api/v3/tasks/insights") {
        return json(res, 200, insights(load(userId)));
      }
      if (req.method === "POST" && url.pathname === "/api/v3/ai/summary") {
        const user = load(userId);
        return json(res, 200, await withGemini(insights(user), user));
      }
      if (req.method === "POST" && url.pathname === "/api/v3/alexa/events") {
        const payload = await bodyOf(req);
        payload.user_id = resolveUser(payload.user_id || userId);
        console.log("Alexa:", payload.intent, payload.task?.title || payload.utterance || "", "home", payload.user_id);
        const spoken = await alexa(load(payload.user_id), payload);
        console.log("Speak:", spoken.speak);
        return json(res, 200, spoken);
      }
      if (req.method === "POST" && url.pathname === "/api/v3/alexa/chime") {
        const payload = await bodyOf(req);
        const user = load(userId);
        const result = await chimeNudge(user, payload, userId, save);
        console.log("Alexa chime:", payload.id, result);
        return json(res, 200, result);
      }
      json(res, 404, { error: "not_found" });
    } catch (error) {
      console.error("API error", error);
      json(res, 500, {
        error: String(error.message || error),
        speak: "Kizen no pudo guardar. Inténtalo otra vez.",
      });
    }
  })
  .listen(PORT, "0.0.0.0", () => {
    console.log(`Kizen API → http://127.0.0.1:${PORT}`);
    console.log("Casa:", HOME_USER);
    console.log("Gemini:", GEMINI ? "sí (GEMINI_API_KEY)" : "no; insights locales");
  });
