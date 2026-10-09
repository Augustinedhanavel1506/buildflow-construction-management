import { useState } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import { useToast } from "../../components/ui/ToastProvider";
import { reportService } from "../../services/reportService";
import { getErrorMessage } from "../../utils/apiError";

function firstDayOfMonth(): string {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
}

export function TallyExportPanel() {
  const { showToast } = useToast();
  const [from, setFrom] = useState(firstDayOfMonth());
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10));
  const [isExporting, setIsExporting] = useState(false);

  async function handleExport() {
    setIsExporting(true);
    try {
      await reportService.downloadTallySalesVouchersXml(from, to);
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsExporting(false);
    }
  }

  return (
    <div className="bf-reports-page__section">
      <h3>Tally Export</h3>
      <Card>
        <p>
          Export GST tax invoices raised in the selected date range as a Tally-compatible Sales Voucher XML file.
          Import it in Tally via Gateway of Tally → Import Data → Vouchers. Review the ledger names created
          (party, sales, and GST ledgers) against your existing chart of accounts before the first import.
        </p>
        <div className="bf-reports-page__section-header">
          <Input label="From" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          <Input label="To" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <Button onClick={handleExport} isLoading={isExporting}>
          Export Sales Vouchers XML
        </Button>
      </Card>
    </div>
  );
}
