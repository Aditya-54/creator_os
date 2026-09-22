import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { getOverview, seedDemoData } from "../api/analytics";
import { extractErrorMessage } from "../api/client";
import { EmptyState, ErrorState, LoadingState } from "../components/States";
import { StatCard } from "../components/StatCard";
import type { OverviewResponse } from "../api/types";
import { formatDate, formatNumber, formatPercent } from "../utils/format";

export function DashboardPage() {
  const [overview, setOverview] = useState<OverviewResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [seeding, setSeeding] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    getOverview()
      .then(setOverview)
      .catch((e) => setError(extractErrorMessage(e)))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleSeed() {
    setSeeding(true);
    try {
      await seedDemoData();
      load();
    } catch (e) {
      setError(extractErrorMessage(e));
    } finally {
      setSeeding(false);
    }
  }

  if (loading) return <LoadingState label="Loading your dashboard..." />;
  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!overview) return null;

  if (overview.totalContent === 0) {
    return (
      <EmptyState
        title="No content yet"
        description="Connect a social account, or load the demo workspace to explore CreatorOS immediately with clearly-labeled synthetic data."
        action={
          <button className="btn btn-primary" onClick={handleSeed} disabled={seeding}>
            {seeding ? "Seeding demo workspace..." : "Load demo workspace"}
          </button>
        }
      />
    );
  }

  const platformData = Object.entries(overview.platformBreakdown).map(([platform, views]) => ({ platform, views }));

  return (
    <div className="page">
      <div className="page-header">
        <h1>Dashboard</h1>
        <button className="btn btn-ghost" onClick={handleSeed} disabled={seeding}>
          {seeding ? "Reseeding..." : "Reseed demo data"}
        </button>
      </div>

      <div className="stat-grid">
        <StatCard label="Total content" value={String(overview.totalContent)} />
        <StatCard label="Total views" value={formatNumber(overview.totalViews)} />
        <StatCard label="Total likes" value={formatNumber(overview.totalLikes)} />
        <StatCard label="Avg engagement" value={formatPercent(overview.avgEngagementRate)} />
      </div>

      <div className="panel-grid">
        <section className="panel">
          <h2>Views by platform</h2>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={platformData}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="platform" stroke="var(--text-muted)" />
              <YAxis stroke="var(--text-muted)" tickFormatter={formatNumber} />
              <Tooltip
                formatter={(v) => formatNumber(Number(v))}
                contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)" }}
              />
              <Bar dataKey="views" fill="var(--accent)" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </section>

        <section className="panel">
          <h2>Top content</h2>
          <ul className="list">
            {overview.topContent.map((c) => (
              <li key={c.id}>
                <Link to={`/content/${c.id}`} className="list-item">
                  <span className="list-item-title">{c.title ?? "Untitled"}</span>
                  <span className="badge">{c.platform}</span>
                  <span className="list-item-metric">{formatNumber(c.views)} views</span>
                  <span className="list-item-metric">{formatPercent(c.engagementRate)} eng.</span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <section className="panel">
        <h2>Recent viral events</h2>
        {overview.recentViralEvents.length === 0 ? (
          <p className="muted">No viral acceleration episodes detected yet.</p>
        ) : (
          <ul className="list">
            {overview.recentViralEvents.map((v) => (
              <li key={v.id}>
                <Link to={`/content/${v.contentId}`} className="list-item">
                  <span className="list-item-title">{v.contentTitle ?? "Untitled"}</span>
                  <span className={`badge badge-${v.confidence.toLowerCase()}`}>{v.confidence}</span>
                  <span className="list-item-metric">{v.multiplier.toFixed(1)}x growth</span>
                  <span className="list-item-metric">{formatDate(v.startTime)}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
