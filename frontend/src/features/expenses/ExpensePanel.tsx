import { useCallback, useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { expenseService } from "../../services/expenseService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { Expense, ExpensePayload } from "../../types/expense";
import { expenseCategoryLabel } from "./categoryLabel";
import { ExpenseForm } from "./ExpenseForm";
import "./ExpensePanel.css";

interface ExpensePanelProps {
  projectId: number;
  onExpensesChanged?: () => void;
}

export function ExpensePanel({ projectId, onExpensesChanged }: ExpensePanelProps) {
  const { user } = useAuth();
  const { showToast } = useToast();
  const [expenses, setExpenses] = useState<Expense[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null);
  const [deletingExpense, setDeletingExpense] = useState<Expense | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const loadExpenses = useCallback(async () => {
    try {
      const data = await expenseService.list(projectId);
      setExpenses(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadExpenses();
  }, [loadExpenses]);

  async function handleCreate(payload: ExpensePayload) {
    await expenseService.create(projectId, payload);
    setIsFormOpen(false);
    showToast("success", "Expense added successfully.");
    await loadExpenses();
    onExpensesChanged?.();
  }

  async function handleUpdate(payload: ExpensePayload) {
    if (!editingExpense) return;
    await expenseService.update(editingExpense.id, payload);
    setEditingExpense(null);
    showToast("success", "Expense updated successfully.");
    await loadExpenses();
    onExpensesChanged?.();
  }

  async function handleDelete() {
    if (!deletingExpense) return;
    setIsDeleting(true);
    try {
      await expenseService.remove(deletingExpense.id);
      showToast("success", "Expense deleted successfully.");
      await loadExpenses();
      onExpensesChanged?.();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsDeleting(false);
      setDeletingExpense(null);
    }
  }

  if (error) {
    return <div className="bf-expense-panel__error">{error}</div>;
  }

  if (expenses === null) {
    return <Skeleton height={240} />;
  }

  const total = expenses.reduce((sum, expense) => sum + Number(expense.amount), 0);

  return (
    <div className="bf-expense-panel">
      <div className="bf-expense-panel__header">
        <div>
          <h3>Expenses</h3>
          <p>Total: {formatCurrency(total)}</p>
        </div>
        {canManage && <Button onClick={() => setIsFormOpen(true)}>+ Add Expense</Button>}
      </div>

      <Card>
        {expenses.length === 0 ? (
          <EmptyState
            title="No expenses recorded yet."
            description="Track material, labour, and other project spending as it happens."
            action={canManage && <Button onClick={() => setIsFormOpen(true)}>+ Add Expense</Button>}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Category</th>
                <th>Supplier</th>
                <th>Description</th>
                <th>Amount</th>
                <th>Added By</th>
                {canManage && <th></th>}
              </tr>
            </thead>
            <tbody>
              {expenses.map((expense) => (
                <tr key={expense.id}>
                  <td>{formatDate(expense.date)}</td>
                  <td>{expenseCategoryLabel(expense.category)}</td>
                  <td>{expense.supplierName ?? "—"}</td>
                  <td>{expense.description ?? "—"}</td>
                  <td>{formatCurrency(expense.amount)}</td>
                  <td>{expense.createdByName}</td>
                  {canManage && (
                    <td className="bf-expense-panel__row-actions">
                      <button onClick={() => setEditingExpense(expense)}>Edit</button>
                      <button onClick={() => setDeletingExpense(expense)}>Delete</button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {isFormOpen && (
        <Modal title="Add Expense" onClose={() => setIsFormOpen(false)}>
          <ExpenseForm onSubmit={handleCreate} onCancel={() => setIsFormOpen(false)} />
        </Modal>
      )}

      {editingExpense && (
        <Modal title="Edit Expense" onClose={() => setEditingExpense(null)}>
          <ExpenseForm initialValue={editingExpense} onSubmit={handleUpdate} onCancel={() => setEditingExpense(null)} />
        </Modal>
      )}

      {deletingExpense && (
        <ConfirmDialog
          title="Delete Expense?"
          description={`This ${formatCurrency(deletingExpense.amount)} expense will be permanently removed.`}
          confirmLabel="Delete"
          isSubmitting={isDeleting}
          onConfirm={handleDelete}
          onCancel={() => setDeletingExpense(null)}
        />
      )}
    </div>
  );
}
