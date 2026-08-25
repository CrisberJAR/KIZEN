const Alexa = require("ask-sdk-core");
const https = require("https");
const http = require("http");
const { URL } = require("url");

/**
 * Pega este archivo COMPLETO en Code → lambda/index.js y pulsa Save + Deploy.
 *
 * Cambia API_BASE por la URL HTTPS del túnel (Cloudflare), sin barra final.
 * Alexa no puede usar localhost. Localtunnel (loca.lt) suele devolver HTML
 * y Alexa lo trata como si hubiera guardado la tarea.
 */
const API_BASE = (process.env.KIZEN_API_BASE || "https://CAMBIA-ESTA-URL.trycloudflare.com").replace(/\/$/, "");
const HOME_USER = (process.env.KIZEN_USER_ID || process.env.KIZEN_HOME_USER || "kizen-casa").trim() || "kizen-casa";

const INTENT_MAP = {
  AddTaskIntent: "ADD_TASK",
  AddTaskIntent: "ADD_TASK",
  CompleteTaskIntent: "COMPLETE_TASK",
  CompleteTaskIntent: "COMPLETE_TASK",
  ListTasksIntent: "LIST_TASKS",
  ListTasksIntent: "LIST_TASKS",
  AddToListIntent: "ADD_TO_LIST",
  AddToListIntent: "ADD_TO_LIST",
  AddHabitIntent: "ADD_HABIT",
  AddHabitIntent: "ADD_HABIT",
  CompleteHabitIntent: "COMPLETE_HABIT",
  CompleteHabitIntent: "COMPLETE_HABIT",
  ListHabitsIntent: "LIST_HABITS",
  ListHabitsIntent: "LIST_HABITS",
  StreakIntent: "STREAK",
  InsightsIntent: "INSIGHTS",
};

function slot(handlerInput, name) {
  const value = Alexa.getSlotValue(handlerInput.requestEnvelope, name);
  return value ? String(value).trim() : "";
}

function titleFrom(handlerInput) {
  return (
    slot(handlerInput, "title") ||
    slot(handlerInput, "habit") ||
    slot(handlerInput, "task") ||
    ""
  );
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

async function sendToKizen(handlerInput, intent, extra = {}) {
  const userId = Alexa.getUserId(handlerInput.requestEnvelope);
  const title = titleFrom(handlerInput);
  const listName = slot(handlerInput, "list");
  const utterance =
    Alexa.getRequestType(handlerInput.requestEnvelope) === "IntentRequest"
      ? handlerInput.requestEnvelope.request.intent.name
      : Alexa.getRequestType(handlerInput.requestEnvelope);

  const body = {
    user_id: HOME_USER,
    alexa_account: userId,
    intent,
    utterance: extra.utterance || title || utterance,
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

const LaunchRequestHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === "LaunchRequest";
  },
  handle(handlerInput) {
    return speakAndListen(
      handlerInput,
      "Hola, soy tu asistente Jarvis. Puedes agregar una tarea, marcar un hábito o preguntarme cómo vas.",
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
    const intent = INTENT_MAP[alexaIntent];
    try {
      const text = await sendToKizen(handlerInput, intent);
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
      "Prueba: agrégame comprar leche, completa el hábito de agua, o cómo voy hoy.",
    );
  },
};

const CancelStopHandler = {
  canHandle(handlerInput) {
    return (
      Alexa.getRequestType(handlerInput.requestEnvelope) === "IntentRequest" &&
      ["AMAZON.CancelIntent", "AMAZON.StopIntent"].includes(
        Alexa.getIntentName(handlerInput.requestEnvelope),
      )
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
    return speakAndListen(handlerInput, "No te seguí. Dime una tarea o un hábito.");
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
