import { useCallback, useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { submitOrQueue } from "../offline/submitOrQueue";
import { dailyReportService } from "../../services/dailyReportService";
import { getErrorMessage } from "../../utils/apiError";
import { formatDate } from "../../utils/format";
import type { DailyReport, DailyReportPayload } from "../../types/dailyReport";
import { DailyReportForm } from "./DailyReportForm";
import "./DailyReportsPanel.css";

export function DailyReportsPanel({ projectId }: { projectId: number }) {
  const { showToast } = useToast();
  const [reports, setReports] = useState<DailyReport[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingReport, setEditingReport] = useState<DailyReport | null>(null);

  const loadReports = useCallback(async () => {
    try {
      const data = await dailyReportService.list(projectId);
      setReports(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  async function handleCreate(payload: DailyReportPayload) {
    const { queued } = await submitOrQueue(
      "DAILY_REPORT",
      `Report for ${payload.reportDate}`,
      projectId,
      payload,
      () => dailyReportService.create(projectId, payload),
    );
    setIsFormOpen(false);
    showToast(
      "success",
      queued ? "You're offline — this report will be sent automatically once you're back online." : "Daily report submitted successfully.",
    );
    if (!queued) {
      await loadReports();
    }
  }

  async function handleUpdate(payload: DailyReportPayload) {
    if (!editingReport) return;
    await dailyReportService.update(editingReport.id, payload);
    setEditingReport(null);
    showToast("success", "Daily report updated successfully.");
    await loadReports();
  }

  if (error) {
    return <div className="bf-daily-reports-panel__error">{error}</div>;
  }

  return (
    <div className="bf-daily-reports-panel">
      <div className="bf-daily-reports-panel__header">
        <h3>Daily Site Reports</h3>
        <Button onClick={() => setIsFormOpen(true)}>+ Submit Report</Button>
      </div>

      {reports === null ? (
        <Skeleton height={240} />
      ) : reports.length === 0 ? (
        <Card>
          <EmptyState
            title="No daily reports yet."
            description="Submit a daily report to keep the project manager updated on site progress."
            action={<Button onClick={() => setIsFormOpen(true)}>+ Submit Report</Button>}
          />
        </Card>
      ) : (
        <div className="bf-daily-reports-panel__timeline">
          {reports.map((report) => (
            <Card key={report.id} className="bf-daily-reports-panel__entry">
              <div className="bf-daily-reports-panel__entry-header">
                <strong>{formatDate(report.reportDate)}</strong>
                <button onClick={() => setEditingReport(report)}>Edit</button>
              </div>

              {report.workersPresent != null && (
                <p className="bf-daily-reports-panel__meta">Workers present: {report.workersPresent}</p>
              )}

              {report.workCompleted && (
                <div className="bf-daily-reports-panel__section">
                  <span>Work Completed</span>
                  <p>{report.workCompleted}</p>
                </div>
              )}

              {report.materialsUsed && (
                <div className="bf-daily-reports-panel__section">
                  <span>Materials Used</span>
                  <p>{report.materialsUsed}</p>
                </div>
              )}

              {report.issues && (
                <div className="bf-daily-reports-panel__section bf-daily-reports-panel__section--issue">
                  <span>⚠ Issues</span>
                  <p>{report.issues}</p>
                </div>
              )}

              {report.notes && (
                <div className="bf-daily-reports-panel__section">
                  <span>Notes</span>
                  <p>{report.notes}</p>
                </div>
              )}

              <p className="bf-daily-reports-panel__submitted-by">Submitted by {report.submittedByName}</p>
            </Card>
          ))}
        </div>
      )}

      {isFormOpen && (
        <Modal title="Submit Daily Report" onClose={() => setIsFormOpen(false)}>
          <DailyReportForm onSubmit={handleCreate} onCancel={() => setIsFormOpen(false)} />
        </Modal>
      )}

      {editingReport && (
        <Modal title="Edit Daily Report" onClose={() => setEditingReport(null)}>
          <DailyReportForm
            initialValue={editingReport}
            onSubmit={handleUpdate}
            onCancel={() => setEditingReport(null)}
          />
        </Modal>
      )}
    </div>
  );
}
