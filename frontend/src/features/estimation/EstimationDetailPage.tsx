import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { boqService } from "../../services/boqService";
import { rateMasterService } from "../../services/rateMasterService";
import { estimationService } from "../../services/estimationService";
import { projectService } from "../../services/projectService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import type { BoqItem, BoqValidationPayload } from "../../types/boq";
import { useAuth } from "../auth/AuthContext";
import { RequestReviewForm } from "../reviews/RequestReviewForm";
import { ValidateBoqForm } from "./ValidateBoqForm";
import type { HouseRequirement, HouseRequirementPayload } from "../../types/estimation";
import type { SiteCheck } from "../../types/siteRules";
import { ComponentCostBreakdown } from "./ComponentCostBreakdown";
import { SiteCheckSummary } from "./SiteCheckSummary";
import { RequestQuotesModal } from "../dealers/RequestQuotesModal";
import { Modal } from "../../components/ui/Modal";
import { HouseRequirementForm } from "./HouseRequirementForm";
import {
  GRADE_LABEL,
  GRADE_TONE,
  ROOF_TYPE_LABEL,
  STRUCTURE_LABEL,
  WALL_MATERIAL_LABEL,
  componentIcon,
  componentLabel,
  estimateSourceLabel,
  estimateSourceTone,
  floorLabel,
} from "./labels";
import "./EstimationDetailPage.css";

function formatQuantity(value: number): string {
  return value.toLocaleString("en-IN", { maximumFractionDigits: 2 });
}

export function EstimationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const requirementId = Number(id);
  const { showToast } = useToast();
  const navigate = useNavigate();
  const { user } = useAuth();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [requirement, setRequirement] = useState<HouseRequirement | null>(null);
  const [items, setItems] = useState<BoqItem[] | null>(null);
  const [estimatedCost, setEstimatedCost] = useState<number | null>(null);
  const [costRange, setCostRange] = useState<{ low: number; high: number } | null>(null);
  const [totalBuiltupArea, setTotalBuiltupArea] = useState<number | null>(null);
  const [unpricedItems, setUnpricedItems] = useState<string[]>([]);
  const [fromDrawing, setFromDrawing] = useState(false);
  const [fromStructure, setFromStructure] = useState(false);
  const [siteCheck, setSiteCheck] = useState<SiteCheck | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [isRequestingQuotes, setIsRequestingQuotes] = useState(false);
  const [isRequestingReview, setIsRequestingReview] = useState(false);
  // "all" validates every preliminary line, a BoqItem validates just that one.
  const [validating, setValidating] = useState<BoqItem | "all" | null>(null);

  const load = useCallback(async () => {
    try {
      const req = await estimationService.get(requirementId);
      setRequirement(req);
      estimationService.getSiteCheck(requirementId).then(setSiteCheck).catch(() => setSiteCheck(null));
      setCostRange(null);
      setUnpricedItems([]);
      setFromDrawing(false);
      setFromStructure(false);

      if (req.status === "ESTIMATED") {
        const [project, boqItems] = await Promise.all([
          projectService.get(req.projectId),
          boqService.list(req.projectId),
        ]);
        setEstimatedCost(project.estimatedCost);
        setItems(boqItems);
        setTotalBuiltupArea(req.floors.reduce((sum, f) => sum + f.floorAreaSqft, 0));
      } else {
        setEstimatedCost(null);
        setItems(null);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [requirementId]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleGenerate() {
    setIsGenerating(true);
    try {
      const result = await estimationService.generateBoq(requirementId);
      setEstimatedCost(result.estimatedCost);
      setCostRange(
        result.estimatedCostLow !== result.estimatedCostHigh
          ? { low: result.estimatedCostLow, high: result.estimatedCostHigh }
          : null,
      );
      setItems(result.items);
      setUnpricedItems(result.unpricedItems);
      setFromDrawing(result.quantityBasis === "DRAWN_PLAN");
      setFromStructure(result.structuralBasis === "DRAWN_STRUCTURE");
      setTotalBuiltupArea(result.totalBuiltupAreaSqft);
      setRequirement((current) => (current ? { ...current, status: "ESTIMATED" } : current));
      showToast("success", "Preliminary estimate generated.");
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsGenerating(false);
    }
  }

  async function handleValidate(payload: BoqValidationPayload) {
    if (!validating || !requirement) return;
    if (validating === "all") {
      await boqService.validateAll(requirement.projectId, payload);
    } else {
      await boqService.validate(validating.id, payload);
    }
    setValidating(null);
    showToast("success", "Validation recorded.");
    await load();
  }

  async function handleStarterRates() {
    setIsGenerating(true);
    try {
      const added = await rateMasterService.addStarterRates();
      showToast("success", `${added.added} starter rates added. Replace them with your local rates when you can.`);
      await handleGenerate();
    } catch (err) {
      showToast("error", getErrorMessage(err));
      setIsGenerating(false);
    }
  }

  async function handleEditSubmit(payload: HouseRequirementPayload) {
    await estimationService.update(requirementId, payload);
    setIsEditing(false);
    showToast("success", "Plan updated — regenerate the estimate to reflect your changes.");
    await load();
  }

  if (error) {
    return <div className="bf-estimation-detail__error">{error}</div>;
  }

  if (!requirement) {
    return <Skeleton height={420} />;
  }

  const plotArea = requirement.plotWidthFt * requirement.plotLengthFt;
  const pendingCount = items?.filter((i) => i.estimateSource === "SYSTEM_PRELIMINARY").length ?? 0;
  const planFloorsArea = requirement.floors.reduce((sum, f) => sum + f.floorAreaSqft, 0);

  return (
    <div className="bf-estimation-detail">
      <Link to="/estimation" className="bf-estimation-detail__back">
        ← All House Plans
      </Link>

      <div className="bf-estimation-detail__header">
        <div>
          <div className="bf-estimation-detail__title-row">
            <h1>{requirement.label}</h1>
            <Badge tone={requirement.status === "ESTIMATED" ? "success" : "neutral"}>
              {requirement.status === "ESTIMATED" ? "Estimated" : "Draft"}
            </Badge>
            <Badge tone={GRADE_TONE[requirement.constructionGrade]}>{GRADE_LABEL[requirement.constructionGrade]}</Badge>
          </div>
          {(requirement.location || requirement.district) && (
            <p className="bf-estimation-detail__location">
              {[requirement.location, requirement.district ? `Rates: ${requirement.district} (default rates where it has none)` : null]
                .filter(Boolean)
                .join(" · ")}
            </p>
          )}
        </div>
        <div className="bf-estimation-detail__header-actions">
          {canManage && (
            <Button variant="secondary" onClick={() => navigate(`/estimation/${requirementId}/plan`)}>
              Design Floor Plan
            </Button>
          )}
          {canManage && (
            <Button variant="secondary" onClick={() => navigate(`/estimation/${requirementId}/what-if`)}>
              What if…
            </Button>
          )}
          {requirement.status === "ESTIMATED" && (
            <Button variant="secondary" onClick={() => navigate(`/estimation/${requirementId}/report`)}>
              Client Report
            </Button>
          )}
          <Button variant="secondary" onClick={() => setIsEditing(true)}>
            Edit Plan
          </Button>
        </div>
      </div>

      {items === null || items.length === 0 ? (
        <Card className="bf-estimation-detail__cta">
          <div>
            <h3>Ready to generate a preliminary estimate?</h3>
            <p>
              Based on {planFloorsArea.toLocaleString("en-IN")} sq.ft of built-up area across {requirement.floors.length}{" "}
              {requirement.floors.length === 1 ? "floor" : "floors"}, we'll calculate material quantities and a cost
              range using your business's rate master.
            </p>
          </div>
          <Button onClick={handleGenerate} isLoading={isGenerating}>
            Generate Preliminary Estimate
          </Button>
        </Card>
      ) : (
        <>
          <Card className="bf-estimation-detail__hero">
            <div>
              <span className="bf-estimation-detail__hero-label">Estimated Project Cost</span>
              <div className="bf-estimation-detail__hero-value">{formatCurrency(estimatedCost)}</div>
              {costRange && (
                <span className="bf-estimation-detail__hero-range">
                  Range: {formatCurrency(costRange.low)} – {formatCurrency(costRange.high)}
                </span>
              )}
              <span className="bf-estimation-detail__hero-note">
                for {totalBuiltupArea?.toLocaleString("en-IN")} sq.ft built-up area
                {fromDrawing && " · wall, plaster, paint and flooring quantities taken from your drawn floor plan"}
                {fromStructure && " · concrete and steel taken from your drawn columns and beams"}
              </span>
            </div>
            <div className="bf-estimation-detail__hero-actions">
              <Button onClick={() => setIsRequestingQuotes(true)}>Request Dealer Quotes</Button>
              {canManage && pendingCount > 0 && (
                <Button variant="secondary" onClick={() => setIsRequestingReview(true)}>
                  Request Engineer Review
                </Button>
              )}
              <Button variant="secondary" onClick={() => window.print()}>
                Print / Save PDF
              </Button>
              <Button variant="secondary" onClick={handleGenerate} isLoading={isGenerating}>
                Regenerate Estimate
              </Button>
            </div>
          </Card>

          {unpricedItems.length > 0 && (
            <div className="bf-estimation-detail__disclaimer">
              Not included in this total because they have no rate yet: {unpricedItems.join(", ")}. Add them in Rate
              Master, or{" "}
              <button className="bf-estimation-detail__link" onClick={handleStarterRates}>load illustrative starter rates</button>{" "}
              and regenerate.
            </div>
          )}

          <Card>
            <h3 className="bf-estimation-detail__section-title">Cost Breakdown by Component</h3>
            <ComponentCostBreakdown items={items} />
          </Card>

          <Card>
            <div className="bf-estimation-detail__boq-header">
              <h3 className="bf-estimation-detail__section-title">Bill of Quantities</h3>
              {canManage && pendingCount > 0 && (
                <Button variant="secondary" onClick={() => setValidating("all")}>
                  Validate all ({pendingCount})
                </Button>
              )}
            </div>
            <div className="bf-estimation-detail__table-wrap">
              <table className="bf-table">
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Component</th>
                    <th>Quantity</th>
                    <th>Rate</th>
                    <th>Amount</th>
                    <th>Source</th>
                    {canManage && <th></th>}
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.id}>
                      <td data-label="Item">{item.itemName}</td>
                      <td data-label="Component">
                        <span className="bf-estimation-detail__component-cell">
                          <span aria-hidden="true">{componentIcon(item.component)}</span>
                          {componentLabel(item.component)}
                        </span>
                      </td>
                      <td data-label="Quantity">
                        {formatQuantity(item.quantity)} {item.unit}
                        {item.quantityLow !== null && item.quantityHigh !== null && (
                          <div className="bf-estimation-detail__qty-range">
                            {formatQuantity(item.quantityLow)}–{formatQuantity(item.quantityHigh)}
                          </div>
                        )}
                      </td>
                      <td data-label="Rate">{formatCurrency(item.rate)}</td>
                      <td data-label="Amount">{formatCurrency(item.estimatedAmount)}</td>
                      <td data-label="Source">
                        <Badge tone={estimateSourceTone(item.estimateSource)}>
                          {estimateSourceLabel(item.estimateSource)}
                        </Badge>
                        {item.validatedBy && (
                          <div className="bf-estimation-detail__qty-range" title={item.validationNote ?? undefined}>
                            {item.validatedBy}
                          </div>
                        )}
                      </td>
                      {canManage && (
                        <td className="bf-estimation-detail__row-action">
                          {item.estimateSource === "SYSTEM_PRELIMINARY" && (
                            <button onClick={() => setValidating(item)}>Validate</button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>

          <div className="bf-estimation-detail__disclaimer">
            ⚠️ This is a preliminary, system-generated estimate based on configurable thumb-rule
            coefficients — not a substitute for a licensed engineer's structural design and quantity survey.
            Get professional validation before construction begins.
          </div>
        </>
      )}

      {siteCheck && (
        <Card>
          <div className="bf-estimation-detail__boq-header">
            <h3 className="bf-estimation-detail__section-title">Site check</h3>
            {canManage && (
              <Button variant="secondary" onClick={() => navigate(`/estimation/${requirementId}/plan`)}>
                Set site rules
              </Button>
            )}
          </div>
          <SiteCheckSummary check={siteCheck} />
        </Card>
      )}

      <Card>
        <h3 className="bf-estimation-detail__section-title">Plot &amp; Floor Summary</h3>
        <div className="bf-estimation-detail__plot-grid">
          <div>
            <span className="bf-estimation-detail__plot-label">Plot Size</span>
            <span>
              {requirement.plotWidthFt} × {requirement.plotLengthFt} ft ({plotArea.toLocaleString("en-IN")} sq.ft)
            </span>
          </div>
          <div>
            <span className="bf-estimation-detail__plot-label">Structure</span>
            <span>{STRUCTURE_LABEL[requirement.structureType]}</span>
          </div>
          <div>
            <span className="bf-estimation-detail__plot-label">Wall Material</span>
            <span>{WALL_MATERIAL_LABEL[requirement.wallMaterial]}</span>
          </div>
          <div>
            <span className="bf-estimation-detail__plot-label">Roof Type</span>
            <span>{ROOF_TYPE_LABEL[requirement.roofType]}</span>
          </div>
        </div>

        <div className="bf-estimation-detail__floors">
          {requirement.floors.map((floor) => {
            const rooms = [
              floor.hasKitchen && "Kitchen",
              floor.hasHall && "Hall",
              floor.hasBalcony && "Balcony",
              floor.hasPoojaRoom && "Pooja Room",
            ].filter(Boolean) as string[];
            return (
              <div className="bf-estimation-detail__floor-row" key={floor.id}>
                <span className="bf-estimation-detail__floor-name">{floorLabel(floor.floorLevel)}</span>
                <span className="bf-estimation-detail__floor-detail">
                  {floor.floorAreaSqft.toLocaleString("en-IN")} sq.ft · {floor.bedroomCount} BR ·{" "}
                  {floor.bathroomCount} bath · {floor.doorCount} doors · {floor.windowCount} windows
                  {rooms.length > 0 ? ` · ${rooms.join(", ")}` : ""}
                </span>
              </div>
            );
          })}
        </div>
      </Card>

      {validating && (
        <Modal title={validating === "all" ? "Validate all preliminary lines" : `Validate ${validating.itemName}`} onClose={() => setValidating(null)}>
          <ValidateBoqForm
            item={validating === "all" ? null : validating}
            pendingCount={pendingCount}
            onSubmit={handleValidate}
            onCancel={() => setValidating(null)}
          />
        </Modal>
      )}

      {isRequestingReview && (
        <Modal title="Request engineer review" onClose={() => setIsRequestingReview(false)}>
          <RequestReviewForm
            projectId={requirement.projectId}
            onCreated={(review) => {
              setIsRequestingReview(false);
              showToast("success", "Review requested.");
              navigate(`/reviews/${review.id}`);
            }}
            onCancel={() => setIsRequestingReview(false)}
          />
        </Modal>
      )}

      {isRequestingQuotes && (
        <Modal title="Request dealer quotes" onClose={() => setIsRequestingQuotes(false)}>
          <RequestQuotesModal
            projectId={requirement.projectId}
            defaultTitle={`${requirement.label} – materials`}
            onCreated={(created) => {
              setIsRequestingQuotes(false);
              showToast("success", "Quote request created.");
              navigate(`/quotes/${created.id}`);
            }}
            onCancel={() => setIsRequestingQuotes(false)}
          />
        </Modal>
      )}

      {isEditing && (
        <div className="bf-estimation-detail__form-overlay">
          <div className="bf-estimation-detail__form-panel">
            <div className="bf-estimation-detail__form-header">
              <h2>Edit House Plan</h2>
              <button
                className="bf-estimation-detail__form-close"
                onClick={() => setIsEditing(false)}
                aria-label="Close"
              >
                ×
              </button>
            </div>
            <HouseRequirementForm
              initialValue={requirement}
              onSubmit={handleEditSubmit}
              onCancel={() => setIsEditing(false)}
            />
          </div>
        </div>
      )}
    </div>
  );
}
