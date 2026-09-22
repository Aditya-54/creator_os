import { useState, type FormEvent } from "react";
import { askAi } from "../api/analytics";
import { extractErrorMessage } from "../api/client";
import type { AiQueryResponse } from "../api/types";
import { formatNumber } from "../utils/format";

interface Turn {
  question: string;
  response?: AiQueryResponse;
  error?: string;
}

const SUGGESTIONS = [
  "Why did my top video suddenly accelerate?",
  "Which topics work best in India?",
  "Compare my football content and gaming content.",
  "What should I test next based on my historical data?",
];

export function AiAnalystPage() {
  const [question, setQuestion] = useState("");
  const [turns, setTurns] = useState<Turn[]>([]);
  const [asking, setAsking] = useState(false);

  async function ask(q: string) {
    if (!q.trim() || asking) return;
    setAsking(true);
    setQuestion("");
    const turn: Turn = { question: q };
    setTurns((prev) => [...prev, turn]);
    try {
      const response = await askAi(q);
      setTurns((prev) => prev.map((t) => (t === turn ? { ...t, response } : t)));
    } catch (e) {
      const message = extractErrorMessage(e);
      setTurns((prev) => prev.map((t) => (t === turn ? { ...t, error: message } : t)));
    } finally {
      setAsking(false);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    ask(question);
  }

  return (
    <div className="page ai-page">
      <div className="page-header">
        <h1>AI Analyst</h1>
      </div>

      {turns.length === 0 && (
        <div className="panel">
          <p className="muted">Ask about your content, or try one of these:</p>
          <div className="suggestion-list">
            {SUGGESTIONS.map((s) => (
              <button key={s} className="btn btn-ghost" onClick={() => ask(s)}>
                {s}
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="chat-thread">
        {turns.map((t, i) => (
          <div key={i} className="chat-turn">
            <div className="chat-bubble chat-question">{t.question}</div>
            {t.error && <div className="chat-bubble chat-error">{t.error}</div>}
            {t.response && (
              <div className="chat-bubble chat-answer">
                {!t.response.aiGenerated && <div className="badge badge-warn">AI provider not configured</div>}
                <p>{t.response.answer}</p>
                {t.response.limitations && <p className="muted small">{t.response.limitations}</p>}
                {t.response.fallbackOverview && (
                  <div className="fallback-overview">
                    <strong>Your current overview:</strong>
                    <ul>
                      <li>{t.response.fallbackOverview.totalContent} content items</li>
                      <li>{formatNumber(t.response.fallbackOverview.totalViews)} total views</li>
                      <li>
                        Top content: {t.response.fallbackOverview.topContent[0]?.title ?? "none yet"}
                      </li>
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
        {asking && <div className="chat-bubble chat-answer chat-pending">Thinking...</div>}
      </div>

      <form className="chat-input-row" onSubmit={handleSubmit}>
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask about your content..."
          disabled={asking}
        />
        <button className="btn btn-primary" type="submit" disabled={asking || !question.trim()}>
          Ask
        </button>
      </form>
    </div>
  );
}
