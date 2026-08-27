/** Recordatorios de Alexa: el teléfono avisa y el Echo suena solo. */

const DEBOUNCE_MS = 90_000;
const RING_IN_SECONDS = 5;
const TZ = process.env.KIZEN_TZ || "America/Lima";

export function clientSnapshot(user) {
  const copy = { ...(user || {}) };
  delete copy.alexa;
  return copy;
}

export function alexaStatus(user) {
  const alexa = user?.alexa;
  if (!alexa || !alexa.access_token) return { linked: false };
  return {
    linked: true,
    has_reminders: Boolean(alexa.has_reminders),
    token_age_min: alexa.updated_at ? Math.round((Date.now() - alexa.updated_at) / 60000) : null,
    last_error: alexa.last_error || null,
  };
}

export function rememberAlexa(user, body) {
  const token = String(body?.api_access_token || "").trim();
  const endpoint = String(body?.api_endpoint || "").trim().replace(/\/$/, "");
  if (!token || !endpoint) return false;
  const prev = user.alexa || {};
  user.alexa = {
    ...prev,
    endpoint,
    access_token: token,
    alexa_user: body.alexa_account || prev.alexa_user || "",
    has_reminders: typeof body.has_reminders === "boolean" ? body.has_reminders : Boolean(prev.has_reminders),
    updated_at: Date.now(),
    alerts: prev.alerts && typeof prev.alerts === "object" ? prev.alerts : {},
    last_chime: prev.last_chime && typeof prev.last_chime === "object" ? prev.last_chime : {},
    last_error: null,
  };
  return true;
}

export async function chimeNudge(user, payload, userId, save) {
  const id = String(payload?.id || "").trim();
  const title = String(payload?.title || "aviso").trim() || "aviso";
  if (!id) return { ok: false, reason: "missing_id" };

  if (payload?.cancel) {
    await deleteAlert(user, id);
    save(userId, user);
    return { ok: true, cancelled: true };
  }

  const nudge = (user.day_nudges || []).find((item) => item.id === id);
  if (nudge && nudge.is_done) {
    await deleteAlert(user, id);
    save(userId, user);
    return { ok: true, skipped: "done" };
  }

  const now = Date.now();
  const last = Number(user.alexa?.last_chime?.[id] || 0);
  if (now - last < DEBOUNCE_MS) return { ok: true, skipped: "debounce" };

  if (!user.alexa?.access_token || !user.alexa?.endpoint) {
    return { ok: false, reason: "no_alexa" };
  }

  await deleteAlert(user, id);
  const created = await createReminder(user.alexa, title, { at: Date.now() + 5_000 });
  if (!user.alexa.alerts) user.alexa.alerts = {};
  if (!user.alexa.last_chime) user.alexa.last_chime = {};
  if (created.alertToken) rememberToken(user, id, created.alertToken);
  user.alexa.last_chime[id] = now;
  user.alexa.last_error = created.error || null;
  save(userId, user);
  if (created.error) return { ok: false, reason: created.error };
  return { ok: true };
}

export async function armPendingNudges(user, userId, save) {
  if (!user.alexa?.access_token || !user.alexa?.endpoint) {
    return { ok: false, reason: "no_alexa", count: 0 };
  }
  const epoch = dayEpoch();
  const pending = (user.day_nudges || []).filter((item) => !item.is_done && Number(item.day_epoch) === epoch);
  if (!pending.length) return { ok: true, count: 0 };

  const deadline = Date.now() + 3500;
  let count = 0;
  let lastError = null;
  for (const nudge of pending) {
    const times = nextFireTimes(nudge, 6);
    for (const when of times) {
      if (Date.now() > deadline) break;
      const created = await createReminder(user.alexa, nudge.title, { at: when });
      if (created.alertToken) {
        rememberToken(user, nudge.id, created.alertToken);
        count += 1;
      }
      if (created.error) lastError = created.error;
    }
  }
  user.alexa.last_error = lastError;
  save(userId, user);
  if (lastError && count === 0) return { ok: false, reason: lastError, count };
  return { ok: true, count };
}

export async function silenceDoneNudges(user, userId, save) {
  const alerts = user.alexa?.alerts || {};
  const ids = Object.keys(alerts);
  if (!ids.length) return;
  let changed = false;
  for (const id of ids) {
    const nudge = (user.day_nudges || []).find((item) => item.id === id);
    if (!nudge || nudge.is_done) {
      await deleteAlert(user, id);
      changed = true;
    }
  }
  if (changed) save(userId, user);
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

function endOfTodayMs() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const num = (type) => Number(parts.find((part) => part.type === type)?.value || 0);
  return Date.UTC(num("year"), num("month") - 1, num("day") + 1) - 1;
}

function nextFireTimes(nudge, limit) {
  const interval = Math.max(5, Number(nudge.interval_minutes || 20)) * 60_000;
  const now = Date.now();
  let next = Number(nudge.start_at || now);
  if (next < now) {
    const steps = Math.floor((now - next) / interval) + 1;
    next += steps * interval;
  }
  if (next < now + 60_000) next = now + 65_000;
  const end = endOfTodayMs();
  const times = [];
  while (times.length < limit && next < end) {
    times.push(next);
    next += interval;
  }
  return times;
}

function formatLocal(ms) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(ms));
  const get = (type) => parts.find((part) => part.type === type)?.value || "00";
  return `${get("year")}-${get("month")}-${get("day")}T${get("hour")}:${get("minute")}:${get("second")}`;
}

function rememberToken(user, id, token) {
  if (!user.alexa.alerts) user.alexa.alerts = {};
  const prev = user.alexa.alerts[id];
  const list = Array.isArray(prev) ? prev : prev ? [prev] : [];
  list.push(token);
  user.alexa.alerts[id] = list.slice(-12);
}

function safeSpeech(title) {
  return String(title || "aviso").replace(/[<>&]/g, " ").replace(/\s+/g, " ").trim().slice(0, 80) || "aviso";
}

async function createReminder(alexa, title, when) {
  const text = `Aviso de Kizen. ${safeSpeech(title)}. Sigue pendiente.`;
  const trigger = when && when.at
    ? {
        type: "SCHEDULED_ABSOLUTE",
        scheduledTime: formatLocal(when.at),
        timeZoneId: TZ,
      }
    : {
        type: "SCHEDULED_RELATIVE",
        offsetInSeconds: Number((when && when.offsetSeconds) || RING_IN_SECONDS),
      };
  const body = {
    requestTime: new Date().toISOString(),
    trigger,
    alertInfo: {
      spokenInfo: {
        content: [
          {
            locale: "es-MX",
            text,
          },
        ],
      },
    },
    pushNotification: { status: "ENABLED" },
  };
  const result = await alexaFetch(alexa, "/v1/alerts/reminders", {
    method: "POST",
    body: JSON.stringify(body),
  });
  if (!result.ok) return { error: result.error || `http_${result.status}` };
  return { alertToken: result.json?.alertToken || "" };
}

async function deleteAlert(user, id) {
  const raw = user.alexa?.alerts?.[id];
  const tokens = Array.isArray(raw) ? raw : raw ? [raw] : [];
  if (user.alexa?.access_token && user.alexa?.endpoint) {
    for (const token of tokens) {
      await alexaFetch(user.alexa, `/v1/alerts/reminders/${encodeURIComponent(token)}`, { method: "DELETE" });
    }
  }
  if (user.alexa?.alerts) delete user.alexa.alerts[id];
}

async function alexaFetch(alexa, path, init) {
  const endpoint = String(alexa.endpoint || "https://api.amazonalexa.com").replace(/\/$/, "");
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 6000);
  try {
    const res = await fetch(`${endpoint}${path}`, {
      ...init,
      headers: {
        Authorization: `Bearer ${alexa.access_token}`,
        "Content-Type": "application/json",
        ...(init.headers || {}),
      },
      signal: controller.signal,
    });
    const raw = await res.text();
    let json = {};
    try {
      json = raw ? JSON.parse(raw) : {};
    } catch {
      json = {};
    }
    if (!res.ok) {
      const error = json.message || json.error || raw.slice(0, 120) || `http_${res.status}`;
      console.error("Alexa reminder", res.status, error);
      return { ok: false, status: res.status, error: String(error), json };
    }
    return { ok: true, status: res.status, json };
  } catch (error) {
    console.error("Alexa reminder", error);
    return { ok: false, status: 0, error: String(error.message || error) };
  } finally {
    clearTimeout(timer);
  }
}
