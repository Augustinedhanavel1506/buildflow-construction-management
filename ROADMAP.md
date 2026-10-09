# BuildFlow roadmap

## Upcoming: visual plan builder (simple CAD for homeowners and engineers)

Goal: a drag-and-drop planning tool where the drawing, quantities, BOQ and cost stay connected,
instead of another general-purpose CAD package. Users never need commands like LINE or TRIM.

The idea in one line: change a room's size and the area, wall lengths, material quantities,
BOQ and cost estimate all update from it.

### Three modes

| Mode | Who | What it offers |
|---|---|---|
| Homeowner | Landowner | Plot, bedrooms, floors, kitchen, parking, budget, location, then "Generate preliminary plan" |
| Engineer | Engineer / architect | Exact dimensions, walls, columns, beams, staircases, setbacks, layers, measurements, DXF/DWG export |
| Contractor | Builder | Plan, BOQ, materials, quantity, rate, cost, dealer quotation |

### Principle

AI may understand the user's natural-language requirements and turn them into structured inputs.
Dimensions, quantities, rates and costs always come from the deterministic rule and engineering
engines, never from the AI.

### Already in place (foundations)

- Room checklist intake, estimation rule engine and BOQ generation (Home Estimator).
- Floor plan editor: suggested layout, drag/resize rooms, doors and windows, saved per floor,
  and "Save & use in estimate" to drive the checklist from the drawing.
- Engineer role, review requests, validation, and read-only plan view for reviewers.
- Quantities taken from the drawing: external and internal wall length and area, plaster and paint
  area, and flooring come from the drawn rooms and openings, replacing the built-up-area thumb rules
  once every floor is drawn.
- "What if" scenarios: change finish level, areas, rooms, floors or balcony and see the cost difference
  by component and material, without saving anything.
- Client report: printable/PDF estimate with plans, cost breakdown, quantities, review status, an
  honest note on unverified coefficients, and signature lines.
- Starter rates: one click loads illustrative rates for every estimating item.
- District rates: a rate can be set per district, an estimate uses its district's rate and falls back to
  the default, and "Copy to district" starts a district from existing rates with a percentage adjustment
  (no regional prices are invented).
- Structural frame on the plan: columns at wall junctions (suggested, draggable, editable), beams derived
  along the walls between them, a slab and ground-floor footings. With columns on every floor, concrete
  and steel come from this frame instead of the per-sq.ft rules.
- Site check: setbacks (shaded on the plan, respected by the layout suggester), plot coverage and floor
  area ratio against limits the user enters, flagged rooms, on the plan, estimate and client report. No
  legal limits are built in; they vary by authority.
- Structural calculators (concrete and steel, masonry, bar weights).
- Dealers, quote requests, quote comparison and award.

### To build

1. **Frame refinement** (balanced column grid with spans ≤15 ft is done; "Every junction" remains as an option):
   per-member sizes and steel,
   separate plinth/roof/lintel beams, staircase and sunshade, and member schedules for the engineer.
2. **Site plan**: building position on the plot, parking and gate, trees and utilities, road width.
3. **Elevations and sections** generated from the plan and floor heights. (Done: 3D home view with colours saved with the house and shown on the client report, furnished rooms, garden, stairs between floors, walk-through, and a printable plan sheet. Still to do: textures, railings, roof shapes for L-shaped houses, elevation drawings.)
4. **Engineer mode**: exact numeric entry for every element, layers, dimension tools, measurement.
5. **Export**: PDF drawing sheet, then DXF/DWG for engineers who continue in AutoCAD or Civil 3D.
6. **Natural-language intake** ("30x50 plot, 3 bedrooms, 35 lakh budget") converted to structured
   requirements for the planner.
7. **BIM interoperability** (Revit, Tekla) as a later-stage integration, not a first goal.

## Other planned work

- Estimate versions and comparison (saved snapshots over time).
- Seeded starter dealers and district rate sets (rates are seeded; dealers are not, because invented
  dealers would mislead).
- Real coefficients from TN PWD SOR / CPWD DSR or completed projects, replacing the placeholders.
- Dealer-facing login; distance-based delivery.
- Engineer and architect marketplace; engineers serving more than one business.
- Drawings and document uploads per project.
