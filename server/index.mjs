import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createHash, randomUUID } from "node:crypto";

const ROOT = path.dirname(fileURLToPath(import.meta.url));
const DATA = process.env.KIZEN_DATA_DIR || path.join(ROOT, "data");
const PORT = Number(process.env.PORT || 8787);
const GEMINI = process.env.GEMINI_API_KEY || "";
const HOME_USER = (process.env.KIZEN_HOME_USER || "kizen-casa").trim() || "kizen-casa";

fs.mkdirSync(DATA, { recursive: true });

const empty = () => ({ lists: [], tasks: [], habits: [], habit_logs: [] });

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

function load(userId) {
  const file = fileFor(userId);
  if (!fs.existsSync(file)) return empty();
  try {
    const data = { ...empty(), ...JSON.parse(fs.readFileSync(file, "utf8")) };
    data.lists = Array.isArray(data.lists) ? data.lists : [];
    data.tasks = Array.isArray(data.tasks) ? data.tasks : [];
    data.habits = Array.isArray(data.habits) ? data.habits : [];
    data.habit_logs = Array.isArray(data.habit_logs) ? data.habit_logs : [];
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

function mergeLogs(localItems, remoteItems) {
  const map = new Map((localItems || []).map((item) => [`${item.habit_id}:${item.day_epoch}`, item]));
  for (const item of remoteItems || []) {
    map.set(`${item.habit_id}:${item.day_epoch}`, item);
  }
  return [...map.values()];
}

function isoDow() {
  return ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"][
    (new Date().getDay() + 6) % 7
  ];
}

function dayEpoch() {
  return Math.floor(Date.now() / 86_400_000);
}

function phrase(done, total, streak, open) {
  if (total === 0 && open === 0) return "Hoy el día está en blanco. Un hábito pequeño basta para empezar.";
  if (done === total && total > 0 && open === 0) return `Todo lo de hoy está listo. Tu mejor racha: ${streak} días. Respira.`;
  if (streak >= 3) return `Racha de ${streak} días. ${done} de ${total} hábitos y ${open} tareas suaves.`;
  return `${done} de ${total} hábitos de hoy. Sin prisa: ${open} tareas te esperan.`;
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
  return {
    text: phrase(done, todayHabits.length, streak, open),
    habits_done_today: done,
    habits_total_today: todayHabits.length,
    best_streak: streak,
    open_tasks: open,
  };
}

async function withGemini(base, user) {
  if (!GEMINI) return base;
  const openTitles = (user.tasks || []).filter((task) => !task.is_done).slice(0, 8).map((task) => task.title).join(", ");
  const doneTitles = (user.tasks || []).filter((task) => task.is_done).slice(-8).map((task) => task.title).join(", ");
  const prompt = `Eres Jarvis, voz suave de Kizen, para un parlante Alexa. En 1 o 2 frases cortas en español, cálidas y motivadoras, resume el día. Hecho: ${doneTitles || "aún nada"}. Pendiente: ${openTitles || "nada"}. Hábitos: ${base.habits_done_today} de ${base.habits_total_today}. Racha: ${base.best_streak}. No digas que eres una IA. Sin asteriscos ni listas.`;
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
  const intent = body.intent || "INSIGHTS";
  const title = body.task?.title || body.utterance || "";
  const now = Date.now();
  const userId = body.user_id;
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
    return { speak: open.length ? `Te quedan: ${open.slice(0, 5).join(", ")}.` : "No tienes tareas pendientes. Respira.", intent };
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
    user.habit_logs.push({
      id: randomUUID(),
      habit_id: habit.id,
      day_epoch: dayEpoch(),
      completed_at: now,
    });
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
        return json(res, 200, { ok: true, gemini: Boolean(GEMINI) });
      }
      if (req.method === "GET" && url.pathname === "/api/v3/sync") {
        return json(res, 200, load(userId));
      }
      if (req.method === "PUT" && url.pathname === "/api/v3/sync") {
        const incoming = await bodyOf(req);
        const local = load(userId);
        const merged = {
          lists: mergeById(local.lists, incoming.lists),
          tasks: mergeById(local.tasks, incoming.tasks),
          habits: mergeById(local.habits, incoming.habits),
          habit_logs: mergeLogs(local.habit_logs, incoming.habit_logs),
        };
        save(userId, merged);
        return json(res, 200, merged);
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
