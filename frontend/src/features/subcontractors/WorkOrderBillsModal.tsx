import { useCallback, useEffect, useState } from "react";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { subcontractorService } from "../../services/subcontractorService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { SubcontractorBill, SubcontractorBillPayload, WorkOrder } from "../../types/subcontractor";
import { SubcontractorBillForm } from "./SubcontractorBillForm";
import { SubcontractorPaymentsModal } from "./SubcontractorPaymentsModal";
import { subBillStatusTone } from "./statusTone";
import "./WorkOrderBillsModal.css";

export function WorkOrderBillsModal({
  workOrder,
  canManage,
  onClose,
  onChanged,
}: {
  workOrder: WorkOrder;
  canManage: boolean;
  onClose: () => void;
  onChanged: () => void;
}) {
  const { showToast } = useToast();
  const [bills, setBills] = useState<SubcontractorBill[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [viewingPaymentsBill, setViewingPaymentsBill] = useState<SubcontractorBill | null>(null);
  const [approvingId, setApprovingId] = useState<number | null>(null);

  const loadBills = useCallback(async () => {
    try {
      const data = await subcontractorService.listBills(workOrder.id);
      setBills(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [workOrder.id]);

  useEffect(() => {
    loadBills();
  }, [loadBills]);

  async function handleCreate(payload: SubcontractorBillPayload) {
    await subcontractorService.createBill(workOrder.id, payload);
    setIsFormOpen(false);
    showToast("success", "Subcontractor bill created successfully.");
    await loadBills();
    onChanged();
  }

  async function handleApprove(bill: SubcontractorBill) {
    setApprovingId(bill.id);
    try {
      await subcontractorService.approveBill(bill.id);
      showToast("success", "Bill approved.");
      await loadBills();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setApprovingId(null);
    }
  }

  async function handlePaymentRecorded() {
    await loadBills();
    onChanged();
    if (viewingPaymentsBill) {
      const refreshed = (await subcontractorService.listBills(workOrder.id)).find((b) => b.id === viewingPaymentsBill.id);
      setViewingPaymentsBill(refreshed ?? null);
    }
    showToast("success", "Payment recorded successfully.");
  }

  const nextBillNumber = `SC-${(bills?.length ?? 0) + 1}`;

  return (
    <Modal title={`Bills — ${workOrder.title}`} onClose={onClose}>
      <div className="bf-work-order-bills-modal">
        {error && <div className="bf-work-order-bills-modal__error">{error}</div>}

        <div className="bf-work-order-bills-modal__header">
          <span>
            Contract Value: <strong>{formatCurrency(workOrder.contractValue)}</strong>
          </span>
          {canManage && <Button onClick={() => setIsFormOpen(true)}>+ New Bill</Button>}
        </div>

        {bills === null ? (
          <Skeleton height={200} />
        ) : bills.length === 0 ? (
          <EmptyState
            title="No bills yet."
            description="Raise a bill against work completed by this subcontractor."
            action={canManage && <Button onClick={() => setIsFormOpen(true)}>+ New Bill</Button>}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Bill No.</th>
                <th>Date</th>
                <th>Net Payable</th>
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
                  <td>{formatCurrency(bill.netPayableAmount)}</td>
                  <td>{formatCurrency(bill.outstandingAmount)}</td>
                  <td>
                    <Badge tone={subBillStatusTone(bill.status)}>{bill.status}</Badge>
                  </td>
                  {canManage && (
                    <td className="bf-work-order-bills-modal__row-actions">
                      {bill.status === "DRAFT" && (
                        <button onClick={() => handleApprove(bill)} disabled={approvingId === bill.id}>
                          Approve
                        </button>
                      )}
                      {(bill.status === "APPROVED" || bill.status === "PAID") && (
                        <button onClick={() => setViewingPaymentsBill(bill)}>Payments</button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {isFormOpen && (
          <SubcontractorBillForm nextBillNumber={nextBillNumber} onSubmit={handleCreate} onCancel={() => setIsFormOpen(false)} />
        )}
      </div>

      {viewingPaymentsBill && (
        <SubcontractorPaymentsModal
          bill={viewingPaymentsBill}
          canRecordPayment={canManage}
          onClose={() => setViewingPaymentsBill(null)}
          onPaymentRecorded={handlePaymentRecorded}
        />
      )}
    </Modal>
  );
}
