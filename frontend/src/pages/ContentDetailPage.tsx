import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  getContent,
  getContentAnalytics,
  getExplanation,
  getTimeline,
  getViralEvents,
} from "../api/analytics";
import { extractErrorMessage } from "../api/client";
import { ErrorState, LoadingState } from "../components/States";
import { StatCard } from "../components/StatCard";
import type {
  ContentAnalyticsResponse,
  ContentResponse,
  ExplanationResponse,
  GrowthPoint,
  ViralEventResponse,
} from "../api/types";
import { formatDate, formatNumber, formatPercent } from "../utils/format";

export function ContentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [content, setContent] = useState<ContentResponse | null>(null);
  const [analytics, setAnalytics] = useState<ContentAnalyticsResponse | null>(null);
  const [timeline, setTimeline] = useState<GrowthPoint[]>([]);
  const [viralEvents, setViralEvents] = useState<ViralEventResponse[]>([]);
  const [explanation, setExplanation] = useState<ExplanationResponse | null>(null);
  const [explaining, setExplaining] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  function load() {
    if (!id) return;
    setLoading(true);
    setError(null);
    Promise.all([getContent(id), getContentAnalytics(id), getTimeline(id), getViralEvents(id)])
      .then(([c, a, t, v]) => {
        setContent(c);
        setAnalytics(a);
        setTimeline(t);
        setViralEvents(v);
      })
      .catch((e) => setError(extractErrorMessage(e)))
      .finally(() => setLoading(false));
  }

  useEffect(load, [id]);

  async function handleExplain() {
    if (!id) return;
    setExplaining(true);
    try {
      setExplanation(await getExplanation(id));
    } catch (e) {
      setError(extractErrorMessage(e));
    } finally {
      setExplaining(false);
    }
  }

  if (loading) return <LoadingState label="Loading content detail..." />;
  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!content || !analytics) return null;

  const chartData = timeline.map((p) => ({ ...p, capturedAtLabel: formatDate(p.capturedAt) }));

  return (
    <div className="page">
      <div className="page-header">
        <h1>{content.title ?? "Untitled"}</h1>
        <span className="badge">{content.platform}</span>
      </div>
      <p className="muted">
        {content.contentType} · {content.country ?? "unknown region"} ·{" "}
        {content.topics.length > 0 ? content.topics.join(", ") : "no topics detected"}
        {content.demo && <span className="badge badge-demo">DEMO DATA</span>}
      </p>

      <div className="stat-grid">
        <StatCard label="Views" value={formatNumber(analytics.totalViews)} />
        <StatCard label="Engagement rate" value={formatPercent(analytics.engagementRate)} />
        <StatCard label="Growth" value={`${formatNumber(analytics.latestGrowthPerHour)}/hr`} />
        <StatCard label="Acceleration" value={`${formatNumber(analytics.latestAccelerationPerHour)}/hr²`} />
      </div>

      <section className="panel">
        <h2>Performance timeline</h2>
        {chartData.length === 0 ? (
          <p className="muted">No historical snapshots yet.</p>
        ) : (
          <ResponsiveContainer width="100%" height={320}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="capturedAtLabel" stroke="var(--text-muted)" minTickGap={40} />
              <YAxis stroke="var(--text-muted)" tickFormatter={formatNumber} />
              <Tooltip
                formatter={(v) => formatNumber(Number(v))}
                contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)" }}
              />
              <Line type="monotone" dataKey="views" stroke="var(--accent)" dot={false} strokeWidth={2} />
              {viralEvents.map((v) => (
                <ReferenceLine
                  key={v.id}
                  x={formatDate(v.startTime)}
                  stroke="var(--warning)"
                  strokeDasharray="4 4"
                  label={{ value: "viral", fill: "var(--warning)", fontSize: 11 }}
                />
              ))}
            </LineChart>
          </ResponsiveContainer>
        )}
      </section>

      <section className="panel">
        <h2>Viral events</h2>
        {viralEvents.length === 0 ? (
          <p className="muted">No viral acceleration episodes detected in the available history.</p>
        ) : (
          <ul className="list">
            {viralEvents.map((v) => (
              <li key={v.id} className="list-item list-item-static">
                <span className={`badge badge-${v.confidence.toLowerCase()}`}>{v.confidence}</span>
                <span className="list-item-metric">{v.multiplier.toFixed(1)}x baseline growth</span>
                <span className="list-item-metric">{formatDate(v.startTime)}</span>
                <p className="muted">{v.explanation}</p>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <div className="panel-header-row">
          <h2>Explain this spike</h2>
          <button className="btn btn-primary" onClick={handleExplain} disabled={explaining}>
            {explaining ? "Analyzing..." : "Explain this spike"}
          </button>
        </div>
        {explanation && (
          <div className="explanation">
            {explanation.turningPoint && (
              <p>
                <strong>Observed turning point:</strong> {formatDate(explanation.turningPoint)}
              </p>
            )}
            <h4>Evidence</h4>
            <ul>
              {explanation.evidence.map((e, i) => (
                <li key={i}>{e}</li>
              ))}
            </ul>
            {explanation.possibleFactors.length > 0 && (
              <>
                <h4>Interpretation</h4>
                <ul>
                  {explanation.possibleFactors.map((e, i) => (
                    <li key={i}>{e}</li>
                  ))}
                </ul>
              </>
            )}
            {explanation.comparisons.length > 0 && (
              <>
                <h4>Comparisons</h4>
                <ul>
                  {explanation.comparisons.map((e, i) => (
                    <li key={i}>{e}</li>
                  ))}
                </ul>
              </>
            )}
            <h4>Limitations</h4>
            <ul className="muted">
              {explanation.limitations.map((e, i) => (
                <li key={i}>{e}</li>
              ))}
            </ul>
          </div>
        )}
      </section>
    </div>
  );
}
