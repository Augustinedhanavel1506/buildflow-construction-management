import { useCallback, useEffect, useState } from "react";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { StatCard } from "../../components/ui/StatCard";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { billingService } from "../../services/billingService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { BillingSummary, RaBill, RaBillPayload } from "../../types/billing";
import { CertifyBillForm } from "./CertifyBillForm";
import { GstInvoiceModal } from "./GstInvoiceModal";
import { PaymentsModal } from "./PaymentsModal";
import { RaBillForm } from "./RaBillForm";
import { billStatusTone } from "./statusTone";
import "./BillingPanel.css";

export function BillingPanel({ projectId }: { projectId: number }) {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [bills, setBills] = useState<RaBill[] | null>(null);
  const [summary, setSummary] = useState<BillingSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingBill, setEditingBill] = useState<RaBill | null>(null);
  const [certifyingBill, setCertifyingBill] = useState<RaBill | null>(null);
  const [viewingPaymentsBill, setViewingPaymentsBill] = useState<RaBill | null>(null);
  const [invoicingBill, setInvoicingBill] = useState<RaBill | null>(null);
  const [submittingId, setSubmittingId] = useState<number | null>(null);

  const loadAll = useCallback(async () => {
    try {
      const [billsData, summaryData] = await Promise.all([
        billingService.listBills(projectId),
        billingService.getSummary(projectId),
      ]);
      setBills(billsData);
      setSummary(summaryData);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  async function handleCreate(payload: RaBillPayload) {
    await billingService.createBill(projectId, payload);
    setIsFormOpen(false);
    showToast("success", "RA bill created successfully.");
    await loadAll();
  }

  async function handleUpdate(payload: RaBillPayload) {
    if (!editingBill) return;
    await billingService.updateBill(editingBill.id, payload);
    setEditingBill(null);
    showToast("success", "RA bill updated successfully.");
    await loadAll();
  }

  async function handleSubmit(bill: RaBill) {
    setSubmittingId(bill.id);
    try {
      await billingService.submitBill(bill.id);
      showToast("success", "RA bill submitted successfully.");
      await loadAll();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setSubmittingId(null);
    }
  }

  async function handleCertify(certifiedAmount: number) {
    if (!certifyingBill) return;
    await billingService.certifyBill(certifyingBill.id, certifiedAmount);
    setCertifyingBill(null);
    showToast("success", "RA bill certified successfully.");
    await loadAll();
  }

  async function handlePaymentRecorded() {
    await loadAll();
    if (viewingPaymentsBill) {
      const refreshed = (await billingService.listBills(projectId)).find((b) => b.id === viewingPaymentsBill.id);
      setViewingPaymentsBill(refreshed ?? null);
    }
    showToast("success", "Payment recorded successfully.");
  }

  if (error) {
    return <div className="bf-billing-panel__error">{error}</div>;
  }

  if (bills === null || summary === null) {
    return <Skeleton height={280} />;
  }

  const nextBillNumber = `RA-${bills.length + 1}`;

  return (
    <div className="bf-billing-panel">
      <div className="bf-billing-panel__header">
        <h3>RA Bills & Payments</h3>
        {canManage && <Button onClick={() => setIsFormOpen(true)}>+ New Bill</Button>}
      </div>

      <div className="bf-billing-panel__stats">
        <StatCard label="Total Billed" value={formatCurrency(summary.totalBilled)} />
        <StatCard label="Total Received" value={formatCurrency(summary.totalReceived)} />
        <StatCard
          label="Outstanding"
          value={<span className={summary.totalOutstanding > 0 ? "bf-billing-panel__outstanding" : undefined}>{formatCurrency(summary.totalOutstanding)}</span>}
        />
        <StatCard label="Retention Held" value={formatCurrency(summary.totalRetentionHeld)} />
      </div>

      <Card>
        {bills.length === 0 ? (
          <EmptyState
            title="No RA bills yet."
            description="Raise a running account bill against completed work to start tracking client payments."
            action={canManage && <Button onClick={() => setIsFormOpen(true)}>+ New Bill</Button>}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Bill No.</th>
                <th>Date</th>
                <th>Work Done</th>
                <th>Net Payable</th>
                <th>Received</th>
                <th>Outstanding</th>
                <th>Status</th>
                {canManage && <th></th>}
              </tr>
            </thead>
            <tbody>
              {bills.map((bill) => (
                <tr key={bill.id}>
                  <td>{bill.billNumber}</td>
                  <td>{formatDate(bill.billDate)}</td>
                  <td>{formatCurrency(bill.certifiedAmount ?? bill.workDoneValue)}</td>
                  <td>{formatCurrency(bill.netPayableAmount)}</td>
                  <td>{formatCurrency(bill.amountReceived)}</td>
                  <td>{formatCurrency(bill.outstandingAmount)}</td>
                  <td>
                    <Badge tone={billStatusTone(bill.status)}>{bill.status}</Badge>
                  </td>
                  {canManage && (
                    <td className="bf-billing-panel__row-actions">
                      {bill.status === "DRAFT" && (
                        <>
                          <button onClick={() => setEditingBill(bill)}>Edit</button>
                          <button onClick={() => handleSubmit(bill)} disabled={submittingId === bill.id}>
                            Submit
                          </button>
                        </>
                      )}
                      {bill.status === "SUBMITTED" && (
                        <button onClick={() => setCertifyingBill(bill)}>Certify</button>
                      )}
                      {(bill.status === "CERTIFIED" || bill.status === "PAID") && (
                        <button onClick={() => setViewingPaymentsBill(bill)}>Payments</button>
                      )}
                      {bill.status !== "DRAFT" && (
                        <button onClick={() => setInvoicingBill(bill)}>Tax Invoice</button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {isFormOpen && (
        <Modal title="New RA Bill" onClose={() => setIsFormOpen(false)}>
          <RaBillForm nextBillNumber={nextBillNumber} onSubmit={handleCreate} onCancel={() => setIsFormOpen(false)} />
        </Modal>
      )}

      {editingBill && (
        <Modal title="Edit RA Bill" onClose={() => setEditingBill(null)}>
          <RaBillForm
            initialValue={editingBill}
            nextBillNumber={nextBillNumber}
            onSubmit={handleUpdate}
            onCancel={() => setEditingBill(null)}
          />
        </Modal>
      )}

      {certifyingBill && (
        <Modal title="Certify RA Bill" onClose={() => setCertifyingBill(null)}>
          <CertifyBillForm bill={certifyingBill} onSubmit={handleCertify} onCancel={() => setCertifyingBill(null)} />
        </Modal>
      )}

      {viewingPaymentsBill && (
        <PaymentsModal
          bill={viewingPaymentsBill}
          canRecordPayment={canManage}
          onClose={() => setViewingPaymentsBill(null)}
          onPaymentRecorded={handlePaymentRecorded}
        />
      )}

      {invoicingBill && <GstInvoiceModal bill={invoicingBill} onClose={() => setInvoicingBill(null)} />}
    </div>
  );
}
