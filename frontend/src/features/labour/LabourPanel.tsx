import { useCallback, useEffect, useMemo, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { submitOrQueue } from "../offline/submitOrQueue";
import { labourService } from "../../services/labourService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import type { AttendanceEntry, LabourCostSummary, Worker, WorkerPayload } from "../../types/labour";
import { WorkerForm } from "./WorkerForm";
import "./LabourPanel.css";

type LabourSubTab = "workers" | "attendance" | "cost";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function LabourPanel({ projectId }: { projectId: number }) {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [subTab, setSubTab] = useState<LabourSubTab>("attendance");
  const [workers, setWorkers] = useState<Worker[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isWorkerFormOpen, setIsWorkerFormOpen] = useState(false);

  const [attendanceDate, setAttendanceDate] = useState(today());
  const [attendance, setAttendance] = useState<AttendanceEntry[] | null>(null);
  const [isSubmittingAttendance, setIsSubmittingAttendance] = useState(false);

  const [costSummary, setCostSummary] = useState<LabourCostSummary | null>(null);

  const loadWorkers = useCallback(async () => {
    try {
      const data = await labourService.listWorkers(projectId);
      setWorkers(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  const loadAttendance = useCallback(async () => {
    try {
      const data = await labourService.getAttendance(projectId, attendanceDate);
      setAttendance(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId, attendanceDate]);

  const loadCostSummary = useCallback(async () => {
    try {
      const data = await labourService.getCostSummary(projectId);
      setCostSummary(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadWorkers();
  }, [loadWorkers]);

  useEffect(() => {
    if (subTab === "attendance") loadAttendance();
  }, [subTab, loadAttendance]);

  useEffect(() => {
    if (subTab === "cost") loadCostSummary();
  }, [subTab, loadCostSummary]);

  async function handleCreateWorker(payload: WorkerPayload) {
    await labourService.createWorker(projectId, payload);
    setIsWorkerFormOpen(false);
    showToast("success", "Worker added successfully.");
    await loadWorkers();
  }

  function toggleAttendance(workerId: number) {
    setAttendance((current) =>
      current
        ? current.map((entry) => (entry.workerId === workerId ? { ...entry, present: !entry.present } : entry))
        : current,
    );
  }

  async function handleSubmitAttendance() {
    if (!attendance) return;
    setIsSubmittingAttendance(true);
    try {
      const payload = {
        date: attendanceDate,
        entries: attendance.map((entry) => ({ workerId: entry.workerId, present: entry.present })),
      };
      const presentCount = attendance.filter((entry) => entry.present).length;
      const { queued } = await submitOrQueue(
        "ATTENDANCE",
        `Attendance for ${attendanceDate} (${presentCount} present)`,
        projectId,
        payload,
        () => labourService.submitAttendance(projectId, payload),
      );
      showToast(
        "success",
        queued
          ? "You're offline — this attendance will be sent automatically once you're back online."
          : "Attendance submitted successfully.",
      );
      if (!queued) {
        await loadAttendance();
      }
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsSubmittingAttendance(false);
    }
  }

  const attendanceByRole = useMemo(() => {
    if (!attendance) return [];
    const groups = new Map<string, AttendanceEntry[]>();
    attendance.forEach((entry) => {
      const list = groups.get(entry.role) ?? [];
      list.push(entry);
      groups.set(entry.role, list);
    });
    return Array.from(groups.entries());
  }, [attendance]);

  if (error) {
    return <div className="bf-labour-panel__error">{error}</div>;
  }

  return (
    <div className="bf-labour-panel">
      <div className="bf-labour-panel__subtabs">
        <button className={subTab === "attendance" ? "bf-labour-panel__subtab--active" : ""} onClick={() => setSubTab("attendance")}>
          Attendance
        </button>
        <button className={subTab === "workers" ? "bf-labour-panel__subtab--active" : ""} onClick={() => setSubTab("workers")}>
          Workers
        </button>
        <button className={subTab === "cost" ? "bf-labour-panel__subtab--active" : ""} onClick={() => setSubTab("cost")}>
          Labour Cost
        </button>
      </div>

      {subTab === "workers" && (
        <div className="bf-labour-panel__section">
          <div className="bf-labour-panel__section-header">
            <h3>Workers</h3>
            {canManage && <Button onClick={() => setIsWorkerFormOpen(true)}>+ Add Worker</Button>}
          </div>
          <Card>
            {workers === null ? (
              <Skeleton height={200} />
            ) : workers.length === 0 ? (
              <EmptyState
                title="No workers added yet."
                description="Add workers to start tracking daily attendance and labour cost."
                action={canManage && <Button onClick={() => setIsWorkerFormOpen(true)}>+ Add Worker</Button>}
              />
            ) : (
              <table className="bf-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Role</th>
                    <th>Daily Rate</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {workers.map((worker) => (
                    <tr key={worker.id}>
                      <td>{worker.name}</td>
                      <td>{worker.role}</td>
                      <td>{formatCurrency(worker.dailyRate)}</td>
                      <td>{worker.active ? "Active" : "Inactive"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>
        </div>
      )}

      {subTab === "attendance" && (
        <div className="bf-labour-panel__section">
          <div className="bf-labour-panel__section-header">
            <h3>Today's Labour</h3>
            <input
              type="date"
              className="bf-labour-panel__date-input"
              value={attendanceDate}
              onChange={(e) => setAttendanceDate(e.target.value)}
            />
          </div>

          {attendance === null ? (
            <Skeleton height={200} />
          ) : attendance.length === 0 ? (
            <Card>
              <EmptyState
                title="No active workers to mark attendance for."
                description="Add workers first, then come back to record daily attendance."
              />
            </Card>
          ) : (
            <>
              {attendanceByRole.map(([role, entries]) => (
                <Card key={role} className="bf-labour-panel__role-card">
                  <div className="bf-labour-panel__role-header">
                    <h4>{role}</h4>
                    <span>Present: {entries.filter((e) => e.present).length}</span>
                  </div>
                  <ul className="bf-labour-panel__worker-list">
                    {entries.map((entry) => (
                      <li key={entry.workerId}>
                        <label>
                          <input
                            type="checkbox"
                            checked={entry.present}
                            onChange={() => toggleAttendance(entry.workerId)}
                          />
                          {entry.workerName}
                        </label>
                      </li>
                    ))}
                  </ul>
                </Card>
              ))}

              <Button onClick={handleSubmitAttendance} isLoading={isSubmittingAttendance}>
                Submit Attendance
              </Button>
            </>
          )}
        </div>
      )}

      {subTab === "cost" && (
        <div className="bf-labour-panel__section">
          <h3>Labour Cost</h3>
          {costSummary === null ? (
            <Skeleton height={200} />
          ) : (
            <Card>
              <div className="bf-labour-panel__total-cost">
                <span>Total Labour Cost</span>
                <strong>{formatCurrency(costSummary.totalCost)}</strong>
              </div>
              {costSummary.byRole.length === 0 ? (
                <EmptyState title="No attendance recorded yet." description="Labour cost is calculated from submitted attendance." />
              ) : (
                <table className="bf-table">
                  <thead>
                    <tr>
                      <th>Role</th>
                      <th>Present Days</th>
                      <th>Cost</th>
                    </tr>
                  </thead>
                  <tbody>
                    {costSummary.byRole.map((row) => (
                      <tr key={row.role}>
                        <td>{row.role}</td>
                        <td>{row.presentDays}</td>
                        <td>{formatCurrency(row.cost)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </Card>
          )}
        </div>
      )}

      {isWorkerFormOpen && (
        <Modal title="Add Worker" onClose={() => setIsWorkerFormOpen(false)}>
          <WorkerForm onSubmit={handleCreateWorker} onCancel={() => setIsWorkerFormOpen(false)} />
        </Modal>
      )}
    </div>
  );
}
