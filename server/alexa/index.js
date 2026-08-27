const Alexa = require("ask-sdk-core");
const https = require("https");
const http = require("http");
const { URL } = require("url");

/**
 * Pega este archivo COMPLETO en Code → lambda/index.js y pulsa Save + Deploy.
 *
 * KIZEN_API_BASE = https://kizen-api.onrender.com
 * Alexa no puede usar localhost.
 *
 * En la consola: Build → Tools → Permissions → activa Reminders.
 */
const API_BASE = (process.env.KIZEN_API_BASE || "https://kizen-api.onrender.com").replace(/\/$/, "");
const HOME_USER = (process.env.KIZEN_USER_ID || process.env.KIZEN_HOME_USER || "kizen-casa").trim() || "kizen-casa";
const REMINDERS_SCOPE = "alexa::alerts:reminders:skill:readwrite";

const INTENT_MAP = {
  AddTaskIntent: "ADD_TASK",
  CompleteTaskIntent: "COMPLETE_TASK",
  ListTasksIntent: "LIST_TASKS",
  AddToListIntent: "ADD_TO_LIST",
  AddHabitIntent: "ADD_HABIT",
  CompleteHabitIntent: "COMPLETE_HABIT",
  ListHabitsIntent: "LIST_HABITS",
  StreakIntent: "STREAK",
  InsightsIntent: "INSIGHTS",
  AddNudgeIntent: "ADD_NUDGE",
  CompleteNudgeIntent: "COMPLETE_NUDGE",
  ListNudgesIntent: "LIST_NUDGES",
};

function slot(handlerInput, name) {
  const value = Alexa.getSlotValue(handlerInput.requestEnvelope, name);
  return value ? String(value).trim() : "";
}

function titleFrom(handlerInput) {
  return (
    slot(handlerInput, "title") ||
    slot(handlerInput, "habit") ||
    slot(handlerInput, "nudge") ||
    slot(handlerInput, "task") ||
    ""
  );
}

function dig(obj, keys) {
  var cur = obj;
  for (var i = 0; i < keys.length; i += 1) {
    if (!cur) return null;
    cur = cur[keys[i]];
  }
  if (cur === null || typeof cur === "undefined") return null;
  return cur;
}

function hasRemindersPermission(envelope) {
  const permissions = dig(envelope, ["context", "System", "user", "permissions"]);
  if (!permissions) return false;
  const scopes = permissions.scopes || {};
  const scope = scopes[REMINDERS_SCOPE] || {};
  const status = String(scope.status || "").toUpperCase();
  if (status === "GRANTED") return true;
  return Boolean(permissions.consentToken);
}

function alexaLink(handlerInput) {
  const system = dig(handlerInput.requestEnvelope, ["context", "System"]) || {};
  return {
    api_endpoint: system.apiEndpoint || "https://api.amazonalexa.com",
    api_access_token: system.apiAccessToken || "",
    has_reminders: hasRemindersPermission(handlerInput.requestEnvelope),
  };
}

function postJson(pathname, body) {
  if (!API_BASE || API_BASE.includes("CAMBIA-ESTA-URL")) {
    return Promise.reject(new Error("Falta KIZEN_API_BASE"));
  }
  const url = new URL(pathname, `${API_BASE}/`);
  const payload = JSON.stringify(body);
  const lib = url.protocol === "http:" ? http : https;
  const options = {
    method: "POST",
    hostname: url.hostname,
    port: url.port || (url.protocol === "http:" ? 80 : 443),
    path: `${url.pathname}${url.search}`,
    headers: {
      "Content-Type": "application/json",
      "Content-Length": Buffer.byteLength(payload),
      "X-Kizen-User-Id": body.user_id,
      "User-Agent": "kizen-alexa-skill",
    },
  };
  return new Promise((resolve, reject) => {
    const req = lib.request(options, (res) => {
      const chunks = [];
      res.on("data", (chunk) => chunks.push(chunk));
      res.on("end", () => {
        const raw = Buffer.concat(chunks).toString("utf8").trim();
        const status = res.statusCode || 0;
        if (!raw || raw.startsWith("<") || raw.startsWith("<!")) {
          reject(new Error(`El túnel devolvió HTML (HTTP ${status})`));
          return;
        }
        let parsed = {};
        try {
          parsed = JSON.parse(raw);
        } catch (error) {
          reject(new Error(`HTTP ${status}: ${raw.slice(0, 180)}`));
          return;
        }
        if (typeof parsed.speak === "string" && parsed.speak.trim()) {
          resolve(parsed);
          return;
        }
        reject(new Error(parsed.error || `HTTP ${status} sin respuesta de Kizen`));
      });
    });
    req.setTimeout(6500, () => {
      req.destroy();
      reject(new Error("Tiempo de espera agotado"));
    });
    req.on("error", reject);
    req.write(payload);
    req.end();
  });
}

function looksLikeAvisos(text) {
  const value = String(text || "").toLowerCase();
  return value.indexOf("aviso") !== -1;
}

function kizenIntent(handlerInput) {
  const name = Alexa.getIntentName(handlerInput.requestEnvelope);
  const mapped = INTENT_MAP[name];
  const title = titleFrom(handlerInput);
  if (looksLikeAvisos(title) && (mapped === "LIST_TASKS" || mapped === "ADD_TASK" || mapped === "INSIGHTS")) {
    return "LIST_NUDGES";
  }
  return mapped;
}

async function sendToKizen(handlerInput, intent, extra) {
  extra = extra || {};
  const userId = Alexa.getUserId(handlerInput.requestEnvelope);
  const title = titleFrom(handlerInput);
  const listName = slot(handlerInput, "list");
  const utterance =
    Alexa.getRequestType(handlerInput.requestEnvelope) === "IntentRequest"
      ? handlerInput.requestEnvelope.request.intent.name
      : Alexa.getRequestType(handlerInput.requestEnvelope);
  const link = alexaLink(handlerInput);

  const body = {
    user_id: HOME_USER,
    alexa_account: userId,
    intent,
    utterance: extra.utterance || title || utterance,
    api_endpoint: link.api_endpoint,
    api_access_token: link.api_access_token,
    has_reminders: link.has_reminders,
    task: title
      ? {
          id: extra.taskId || `alexa-${Date.now()}`,
          list_id: extra.listId || "list-personal",
          title,
          notes: listName ? `Lista: ${listName}` : "",
          priority: "MEDIUM",
          is_done: false,
          due_at: null,
          reminder_at: null,
          completed_at: null,
          created_at: Date.now(),
          updated_at: Date.now(),
          source: "ALEXA",
          subtasks: [],
        }
      : null,
    task_id: extra.taskId || null,
    occurred_at: Date.now(),
  };

  const result = await postJson("/api/v3/alexa/events", body);
  return result.speak;
}

function speak(handlerInput, text) {
  return handlerInput.responseBuilder.speak(text).getResponse();
}

function speakAndListen(handlerInput, text) {
  return handlerInput.responseBuilder.speak(text).reprompt("¿Qué más hacemos?").getResponse();
}

function askRemindersPermission(handlerInput, text) {
  return handlerInput.responseBuilder
    .speak(text)
    .withAskForPermissionsConsentCard([REMINDERS_SCOPE])
    .getResponse();
}

const LaunchRequestHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === "LaunchRequest";
  },
  async handle(handlerInput) {
    try {
      await sendToKizen(handlerInput, "LINK_ALEXA");
    } catch (error) {
      console.error("Kizen link error", error);
    }
    if (!hasRemindersPermission(handlerInput.requestEnvelope)) {
      return askRemindersPermission(
        handlerInput,
        "Para que este Echo suene solo con tus avisos de Kizen, acepta el permiso de recordatorios en la app de Alexa.",
      );
    }
    return speakAndListen(
      handlerInput,
      "Hola, soy tu asistente Jarvis. Puedes agregar una tarea, un aviso de hoy, marcar un hábito o preguntarme cómo vas.",
    );
  },
};

const KizenIntentHandler = {
  canHandle(handlerInput) {
    if (Alexa.getRequestType(handlerInput.requestEnvelope) !== "IntentRequest") return false;
    return Boolean(INTENT_MAP[Alexa.getIntentName(handlerInput.requestEnvelope)]);
  },
  async handle(handlerInput) {
    const alexaIntent = Alexa.getIntentName(handlerInput.requestEnvelope);
    const intent = kizenIntent(handlerInput);
    try {
      const text = await sendToKizen(handlerInput, intent);
      if (
        (intent === "ADD_NUDGE" || intent === "LINK_ALEXA") &&
        !hasRemindersPermission(handlerInput.requestEnvelope)
      ) {
        return askRemindersPermission(
          handlerInput,
          `${text} Para que el Echo suene solo, acepta el permiso de recordatorios en la app de Alexa.`,
        );
      }
      return speakAndListen(handlerInput, text);
    } catch (error) {
      console.error("Kizen API error", error);
      return speakAndListen(
        handlerInput,
        "No pude hablar con Kizen. El servidor en la nube no está activo.",
      );
    }
  },
};

const HelpIntentHandler = {
  canHandle(handlerInput) {
    return (
      Alexa.getRequestType(handlerInput.requestEnvelope) === "IntentRequest" &&
      Alexa.getIntentName(handlerInput.requestEnvelope) === "AMAZON.HelpIntent"
    );
  },
  handle(handlerInput) {
    return speakAndListen(
      handlerInput,
      "Prueba: agrégame comprar leche, avísame tomar agua, completa el hábito de agua, o cómo voy hoy.",
    );
  },
};

const CancelStopHandler = {
  canHandle(handlerInput) {
    return (
      Alexa.getRequestType(handlerInput.requestEnvelope) === "IntentRequest" &&
      ["AMAZON.CancelIntent", "AMAZON.StopIntent"].indexOf(
        Alexa.getIntentName(handlerInput.requestEnvelope),
      ) >= 0
    );
  },
  handle(handlerInput) {
    return speak(handlerInput, "Hasta luego.");
  },
};

const FallbackHandler = {
  canHandle(handlerInput) {
    return (
      Alexa.getRequestType(handlerInput.requestEnvelope) === "IntentRequest" &&
      Alexa.getIntentName(handlerInput.requestEnvelope) === "AMAZON.FallbackIntent"
    );
  },
  handle(handlerInput) {
    return speakAndListen(handlerInput, "No te seguí. Dime una tarea, un hábito o un aviso de hoy.");
  },
};

const SessionEndedHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === "SessionEndedRequest";
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder.getResponse();
  },
};

const ErrorHandler = {
  canHandle() {
    return true;
  },
  handle(handlerInput, error) {
    console.error(error);
    return speak(handlerInput, "Algo se trabó. Inténtalo otra vez.");
  },
};

exports.handler = Alexa.SkillBuilders.custom()
  .addRequestHandlers(
    LaunchRequestHandler,
    KizenIntentHandler,
    HelpIntentHandler,
    CancelStopHandler,
    FallbackHandler,
    SessionEndedHandler,
  )
  .addErrorHandlers(ErrorHandler)
  .lambda();
