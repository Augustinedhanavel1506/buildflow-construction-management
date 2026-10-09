import { useCallback, useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { billingService } from "../../services/billingService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { BillPayment, BillPaymentPayload, RaBill } from "../../types/billing";
import { BillPaymentForm } from "./BillPaymentForm";
import "./PaymentsModal.css";

export function PaymentsModal({
  bill,
  canRecordPayment,
  onClose,
  onPaymentRecorded,
}: {
  bill: RaBill;
  canRecordPayment: boolean;
  onClose: () => void;
  onPaymentRecorded: () => void;
}) {
  const [payments, setPayments] = useState<BillPayment[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const loadPayments = useCallback(async () => {
    try {
      const data = await billingService.listPayments(bill.id);
      setPayments(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [bill.id]);

  useEffect(() => {
    loadPayments();
  }, [loadPayments]);

  async function handleCreate(payload: BillPaymentPayload) {
    await billingService.createPayment(bill.id, payload);
    setIsFormOpen(false);
    await loadPayments();
    onPaymentRecorded();
  }

  return (
    <Modal title={`Payments — ${bill.billNumber}`} onClose={onClose}>
      <div className="bf-payments-modal">
        {error && <div className="bf-payments-modal__error">{error}</div>}

        <div className="bf-payments-modal__summary">
          <span>Net Payable: {formatCurrency(bill.netPayableAmount)}</span>
          <span>Received: {formatCurrency(bill.amountReceived)}</span>
          <span>Outstanding: {formatCurrency(bill.outstandingAmount)}</span>
        </div>

        {payments === null ? (
          <Skeleton height={100} />
        ) : payments.length === 0 ? (
          <EmptyState title="No payments recorded yet." description="" />
        ) : (
          <ul className="bf-payments-modal__list">
            {payments.map((payment) => (
              <li key={payment.id}>
                <span>{formatCurrency(payment.amount)}</span>
                <span>
                  {payment.mode.replace("_", " ")} · {formatDate(payment.paymentDate)}
                  {payment.referenceNumber ? ` · ${payment.referenceNumber}` : ""}
                </span>
              </li>
            ))}
          </ul>
        )}

        {canRecordPayment && bill.outstandingAmount > 0 && !isFormOpen && (
          <Button onClick={() => setIsFormOpen(true)}>+ Record Payment</Button>
        )}

        {isFormOpen && (
          <BillPaymentForm
            maxAmount={bill.outstandingAmount}
            onSubmit={handleCreate}
            onCancel={() => setIsFormOpen(false)}
          />
        )}
      </div>
    </Modal>
  );
}
