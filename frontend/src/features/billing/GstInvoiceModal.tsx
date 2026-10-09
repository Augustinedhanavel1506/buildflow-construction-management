import { useCallback, useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { businessService } from "../../services/businessService";
import { gstInvoiceService } from "../../services/gstInvoiceService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { RaBill } from "../../types/billing";
import type { GstInvoice, GstInvoicePayload } from "../../types/gstInvoice";
import { GstInvoiceForm } from "./GstInvoiceForm";
import "./GstInvoiceModal.css";

export function GstInvoiceModal({ bill, onClose }: { bill: RaBill; onClose: () => void }) {
  const [invoice, setInvoice] = useState<GstInvoice | null | undefined>(undefined);
  const [defaultGstRate, setDefaultGstRate] = useState(18);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [existing, business] = await Promise.all([
        gstInvoiceService.getForBill(bill.id),
        businessService.get(),
      ]);
      setInvoice(existing);
      setDefaultGstRate(business.defaultGstRate);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [bill.id]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleCreate(payload: GstInvoicePayload) {
    const created = await gstInvoiceService.create(bill.id, payload);
    setInvoice(created);
  }

  return (
    <Modal title={`Tax Invoice — ${bill.billNumber}`} onClose={onClose}>
      <div className="bf-gst-invoice-modal">
        {error && <div className="bf-gst-invoice-modal__error">{error}</div>}

        {invoice === undefined ? (
          <Skeleton height={200} />
        ) : invoice === null ? (
          <GstInvoiceForm
            suggestedInvoiceNumber={`INV-${bill.billNumber}`}
            defaultGstRate={defaultGstRate}
            onSubmit={handleCreate}
            onCancel={onClose}
          />
        ) : (
          <GstInvoicePrintView invoice={invoice} />
        )}
      </div>
    </Modal>
  );
}

function GstInvoicePrintView({ invoice }: { invoice: GstInvoice }) {
  return (
    <div className="bf-gst-invoice-print">
      <div className="bf-gst-invoice-print__actions">
        <Button onClick={() => window.print()}>Print / Save as PDF</Button>
      </div>

      <div className="bf-gst-invoice-print__sheet">
        <h2>Tax Invoice</h2>
        <div className="bf-gst-invoice-print__parties">
          <div>
            <strong>{invoice.supplierName}</strong>
            {invoice.supplierGstin && <div>GSTIN: {invoice.supplierGstin}</div>}
            {invoice.supplierAddress && <div>{invoice.supplierAddress}</div>}
          </div>
          <div>
            <div>Invoice No: {invoice.invoiceNumber}</div>
            <div>Invoice Date: {formatDate(invoice.invoiceDate)}</div>
            <div>RA Bill: {invoice.billNumber}</div>
          </div>
        </div>

        <div className="bf-gst-invoice-print__parties">
          <div>
            <strong>Bill To</strong>
            <div>{invoice.clientName ?? invoice.projectName}</div>
            {invoice.clientGstin && <div>GSTIN: {invoice.clientGstin}</div>}
            {invoice.clientAddress && <div>{invoice.clientAddress}</div>}
          </div>
          <div>
            <div>Place of Supply: {invoice.placeOfSupply ?? "—"}</div>
            <div>Supply Type: {invoice.interState ? "Inter-state" : "Intra-state"}</div>
          </div>
        </div>

        <table className="bf-table">
          <thead>
            <tr>
              <th>Description</th>
              <th>HSN/SAC</th>
              <th>Taxable Value</th>
              <th>GST Rate</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>{invoice.description}</td>
              <td>{invoice.hsnSacCode}</td>
              <td>{formatCurrency(invoice.taxableValue)}</td>
              <td>{invoice.gstRate}%</td>
            </tr>
          </tbody>
        </table>

        <dl className="bf-gst-invoice-print__totals">
          {invoice.interState ? (
            <div>
              <dt>IGST</dt>
              <dd>{formatCurrency(invoice.igstAmount)}</dd>
            </div>
          ) : (
            <>
              <div>
                <dt>CGST</dt>
                <dd>{formatCurrency(invoice.cgstAmount)}</dd>
              </div>
              <div>
                <dt>SGST</dt>
                <dd>{formatCurrency(invoice.sgstAmount)}</dd>
              </div>
            </>
          )}
          <div className="bf-gst-invoice-print__grand-total">
            <dt>Total Invoice Value</dt>
            <dd>{formatCurrency(invoice.totalInvoiceValue)}</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}
