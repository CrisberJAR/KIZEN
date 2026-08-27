/** Recordatorios de Alexa: el teléfono avisa y el Echo suena solo. */

const DEBOUNCE_MS = 90_000;
const RING_IN_SECONDS = 60;

export function clientSnapshot(user) {
  const copy = { ...(user || {}) };
  delete copy.alexa;
  return copy;
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
  if (nudge?.is_done) {
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
  const created = await createReminder(user.alexa, title);
  if (!user.alexa.alerts) user.alexa.alerts = {};
  if (!user.alexa.last_chime) user.alexa.last_chime = {};
  if (created.alertToken) user.alexa.alerts[id] = created.alertToken;
  user.alexa.last_chime[id] = now;
  user.alexa.last_error = created.error || null;
  save(userId, user);
  if (created.error) return { ok: false, reason: created.error };
  return { ok: true };
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

function safeSpeech(title) {
  return String(title || "aviso").replace(/[<>&]/g, " ").replace(/\s+/g, " ").trim().slice(0, 80) || "aviso";
}

async function createReminder(alexa, title) {
  const text = `Aviso de Kizen. ${safeSpeech(title)}. Sigue pendiente.`;
  const body = {
    requestTime: new Date().toISOString(),
    trigger: {
      type: "SCHEDULED_RELATIVE",
      offsetInSeconds: RING_IN_SECONDS,
    },
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
  const token = user.alexa?.alerts?.[id];
  if (token && user.alexa?.access_token && user.alexa?.endpoint) {
    await alexaFetch(user.alexa, `/v1/alerts/reminders/${encodeURIComponent(token)}`, { method: "DELETE" });
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
