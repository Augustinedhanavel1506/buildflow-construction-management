import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { Expense, ExpensePayload } from "../../types/expense";
import { EXPENSE_CATEGORIES, expenseCategoryLabel } from "./categoryLabel";
import "./ExpenseForm.css";

interface ExpenseFormProps {
  initialValue?: Expense;
  onSubmit: (payload: ExpensePayload) => Promise<void>;
  onCancel: () => void;
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function ExpenseForm({ initialValue, onSubmit, onCancel }: ExpenseFormProps) {
  const [category, setCategory] = useState(initialValue?.category ?? "MATERIAL");
  const [amount, setAmount] = useState(initialValue?.amount?.toString() ?? "");
  const [date, setDate] = useState(initialValue?.date ?? today());
  const [supplierName, setSupplierName] = useState(initialValue?.supplierName ?? "");
  const [description, setDescription] = useState(initialValue?.description ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsedAmount = Number(amount);
    if (!amount || Number.isNaN(parsedAmount) || parsedAmount <= 0) {
      setError("Please enter a valid amount.");
      return;
    }
    if (!date) {
      setError("Please select a date.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        category,
        amount: parsedAmount,
        date,
        supplierName: supplierName.trim() || undefined,
        description: description.trim() || undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-expense-form" onSubmit={handleSubmit}>
      {error && <div className="bf-expense-form__error">{error}</div>}

      <div className="bf-expense-form__row">
        <Select label="Category" value={category} onChange={(e) => setCategory(e.target.value as typeof category)}>
          {EXPENSE_CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {expenseCategoryLabel(c)}
            </option>
          ))}
        </Select>
        <Input
          label="Amount (₹)"
          type="number"
          min="0"
          step="0.01"
          required
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
      </div>

      <Input label="Date" type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
      <Input label="Supplier" value={supplierName} onChange={(e) => setSupplierName(e.target.value)} />
      <Input label="Description" value={description} onChange={(e) => setDescription(e.target.value)} />

      <div className="bf-expense-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          {initialValue ? "Save Changes" : "Add Expense"}
        </Button>
      </div>
    </form>
  );
}
