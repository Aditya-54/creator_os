import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listContent } from "../api/analytics";
import { extractErrorMessage } from "../api/client";
import { EmptyState, ErrorState, LoadingState } from "../components/States";
import type { ContentResponse, PageResponse } from "../api/types";
import { formatDate } from "../utils/format";

export function ContentExplorerPage() {
  const [page, setPage] = useState<PageResponse<ContentResponse> | null>(null);
  const [pageIndex, setPageIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  function load(p: number) {
    setLoading(true);
    setError(null);
    listContent(p, 20)
      .then(setPage)
      .catch((e) => setError(extractErrorMessage(e)))
      .finally(() => setLoading(false));
  }

  useEffect(() => load(pageIndex), [pageIndex]);

  if (loading) return <LoadingState label="Loading content..." />;
  if (error) return <ErrorState message={error} onRetry={() => load(pageIndex)} />;
  if (!page || page.content.length === 0) {
    return <EmptyState title="No content yet" description="Connect an account or seed demo data from the dashboard." />;
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Content Explorer</h1>
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Platform</th>
            <th>Type</th>
            <th>Topics</th>
            <th>Country</th>
            <th>Published</th>
          </tr>
        </thead>
        <tbody>
          {page.content.map((c) => (
            <tr key={c.id}>
              <td>
                <Link to={`/content/${c.id}`}>{c.title ?? "Untitled"}</Link>
                {c.demo && <span className="badge badge-demo">DEMO</span>}
              </td>
              <td>
                <span className="badge">{c.platform}</span>
              </td>
              <td>{c.contentType}</td>
              <td>{c.topics.join(", ") || "-"}</td>
              <td>{c.country ?? "-"}</td>
              <td>{formatDate(c.publishedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="pagination">
        <button className="btn" disabled={page.number === 0} onClick={() => setPageIndex(pageIndex - 1)}>
          Previous
        </button>
        <span>
          Page {page.number + 1} of {Math.max(page.totalPages, 1)}
        </span>
        <button
          className="btn"
          disabled={page.number + 1 >= page.totalPages}
          onClick={() => setPageIndex(pageIndex + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
