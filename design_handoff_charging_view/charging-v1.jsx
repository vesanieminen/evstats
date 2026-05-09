// charging-v1.jsx — VARIANT 1: "Refined"
// Conservative tightening of the existing design.
// • Vehicle chip merges car silhouette + name + change affordance into ONE
//   tap target (replaces the separate "Change Vehicle" link).
// • Three-stat header is GONE; current/target now live as endpoints on the
//   SoC slider, "Range +" lives below the slider beside "Adding kWh".
// • Change-vehicle is a bottom-sheet picker, not an inline expand.
// • All units scale with `density` so the same component renders correctly
//   at in-car (1152×1536), tablet, desktop, mobile.

// SectionIcon — inline SVGs matching the originals (plug, sunburst, calendar,
// euro, clock). Single color, sized to the label so they line up on the
// baseline. Reuses the vehicle accent for the section ones; a muted color
// for the in-row meta icons (duration clock, spot-price square).
function SectionIcon({ name, color, size = 16 }) {
  const common = { width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke: color, strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round',
    style: { display: 'block', flex: '0 0 auto' } };
  if (name === 'plug') return (
    <svg {...common}>
      <path d="M9 2v6" /><path d="M15 2v6" />
      <path d="M6 8h12v3a6 6 0 0 1-6 6 6 6 0 0 1-6-6V8z" />
      <path d="M12 17v5" />
    </svg>
  );
  if (name === 'speed') return (
    <svg {...common}>
      <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z" fill={color} stroke={color} />
    </svg>
  );
  if (name === 'calendar') return (
    <svg {...common}>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M3 10h18" /><path d="M8 3v4" /><path d="M16 3v4" />
    </svg>
  );
  if (name === 'euro') return (
    <svg {...common}>
      <path d="M18 7a6 6 0 0 0-10 4" />
      <path d="M18 17a6 6 0 0 1-10-4" />
      <path d="M5 10h8" /><path d="M5 14h8" />
    </svg>
  );
  if (name === 'clock') return (
    <svg {...common}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </svg>
  );
  return null;
}

// SectionHeader — icon + uppercase label on the left, optional meta on right.
// Replaces the bare textual headers so the redesign keeps the original
// iconographic vocabulary (plug / spark / calendar / €).
function SectionHeader({ icon, label, accent, color, D, T, right }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      marginBottom: D.gap * 0.6 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: D.label * 0.5 }}>
        <SectionIcon name={icon} color={accent} size={Math.round(D.label * 1.2)} />
        <div style={{ fontSize: D.label, color: T.textMuted, textTransform: 'uppercase', letterSpacing: 1 }}>{label}</div>
      </div>
      {right}
    </div>
  );
}

function ChargingV1({ density = 'desktop', dark = false }) {
  const s = useChargingState();
  const D = DENSITY[density];
  const T = theme(dark);
  const A = accentRamp(s.vehicle.accent, dark);
  const [sheet, setSheet] = React.useState(null); // 'vehicle' | 'schedule' | null

  const Card = ({ children, style }) => (
    <div style={{
      background: T.surface, borderRadius: D.radius, padding: D.pad,
      border: `1px solid ${T.line}`, ...style,
    }}>{children}</div>
  );

  return (
    <div style={{
      width: '100%', height: '100%', overflow: 'auto',
      background: T.bg, color: T.text, fontFamily: 'Inter, -apple-system, system-ui, sans-serif',
      fontSize: D.base, padding: D.pad, boxSizing: 'border-box',
      display: 'flex', flexDirection: 'column', gap: D.gap,
    }}>
      {/* Vehicle chip — single tap target */}
      <button onClick={() => setSheet('vehicle')}
        style={{
          background: T.surface, border: `1px solid ${T.line}`, borderRadius: D.radius,
          padding: `${D.pad * 0.7}px ${D.pad}px`, display: 'flex', alignItems: 'center',
          gap: D.gap, cursor: 'pointer', color: T.text, textAlign: 'left',
          width: '100%', boxSizing: 'border-box',
        }}>
        <CarSilhouette sketch={s.vehicle.sketch} color={A.base} width={D.h2 * 1.6} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: D.h3, fontWeight: 600, letterSpacing: -0.3 }}>{s.vehicle.name}</div>
          <div style={{ fontSize: D.label, color: T.textMuted, marginTop: 2 }}>
            {s.vehicle.trim} · {s.vehicle.battery} kWh
          </div>
        </div>
        <div style={{
          fontSize: D.label, color: T.textMuted, padding: `${D.label * 0.4}px ${D.label * 0.8}px`,
          border: `1px solid ${T.line}`, borderRadius: 999,
        }}>Change ›</div>
      </button>

      {/* Charge level + range — combined */}
      <Card>
        <SectionHeader icon="plug" label="Charge level" accent={A.base} D={D} T={T}
          right={<div style={{ fontSize: D.label, color: T.textMuted }}>{s.vehicle.battery} kWh battery</div>} />
        {/* Stats row above slider — current %, range +, target % */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', alignItems: 'baseline',
          marginBottom: D.gap * 0.6 }}>
          <div>
            <div style={{ fontSize: D.label, color: T.textMuted }}>Current</div>
            <div style={{ fontSize: D.h2, fontWeight: 700, lineHeight: 1, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
              {s.currentPct}<span style={{ fontSize: D.h3, color: T.textMuted }}>%</span>
            </div>
            <div style={{ fontSize: D.label, color: T.textMuted, marginTop: 4 }}>{Math.round(s.out.currentKm)} km</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: D.label, color: T.textMuted }}>Adds</div>
            <div style={{ fontSize: D.h2, fontWeight: 700, color: A.base, lineHeight: 1, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
              +{Math.round(s.out.rangeKm)}<span style={{ fontSize: D.h3 }}> km</span>
            </div>
            <div style={{ fontSize: D.label, color: T.textMuted, marginTop: 4 }}>
              {s.out.addedKwh.toFixed(1)} kWh
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: D.label, color: T.textMuted }}>Target</div>
            <div style={{ fontSize: D.h2, fontWeight: 700, lineHeight: 1, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
              {s.targetPct}<span style={{ fontSize: D.h3, color: T.textMuted }}>%</span>
            </div>
            <div style={{ fontSize: D.label, color: T.textMuted, marginTop: 4 }}>{Math.round(s.out.targetKm)} km</div>
          </div>
        </div>
        <DualSlider low={s.currentPct} high={s.targetPct} onChange={s.setSoc}
          accent={A.base} track={T.track}
          height={Math.round(D.base * 0.7)} thumbSize={Math.round(D.control * 0.45)} />
      </Card>

      {/* Charging speed */}
      <Card>
        <SectionHeader icon="speed" label="Charging speed" accent={A.base} D={D} T={T}
          right={<div style={{ fontSize: D.h3, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
            {s.out.power.toFixed(1)} kW
          </div>} />
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 4 }}>
          <div style={{ fontSize: D.h2, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>{s.amps}</div>
          <div style={{ fontSize: D.h3, color: T.textMuted }}>A · {s.phases}-phase</div>
        </div>
        <SingleSlider value={s.amps} min={0} max={32} onChange={s.setAmps}
          accent={A.base} track={T.track}
          height={Math.round(D.base * 0.6)} thumbSize={Math.round(D.control * 0.4)} />
        <div style={{ marginTop: D.gap * 0.5 }}>
          <AdvancedSettings s={s} D={D} T={T} accent={A.base} />
        </div>
      </Card>

      {/* Schedule */}
      <Card>
        <SectionHeader icon="calendar" label="Schedule" accent={A.base} D={D} T={T}
          right={<div style={{ fontSize: D.label, color: T.textMuted }}>
            Solving for <button onClick={() => s.setCalcMode(s.calcMode === 'end' ? 'start' : 'end')}
              style={{ background: 'none', border: 'none', color: A.base, fontWeight: 600, cursor: 'pointer', padding: 0,
                fontSize: D.label, fontFamily: 'inherit' }}>
              {s.calcMode === 'end' ? 'end time ↻' : 'start time ↻'}
            </button>
          </div>} />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr auto 1fr', alignItems: 'center', gap: D.gap * 0.5 }}>
          <TimeField label="Start" hour={s.startHour} editable={s.calcMode === 'start'}
            onChange={s.setStartHour} D={D} T={T} A={A} dim={s.calcMode === 'end'} />
          <div style={{ color: T.textFaint, fontSize: D.h3, paddingTop: D.label * 1.2 }}>→</div>
          <TimeField label="End" hour={s.endHour} editable={s.calcMode === 'end'}
            onChange={s.setEndHour} D={D} T={T} A={A} dim={s.calcMode === 'start'} />
        </div>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          marginTop: D.gap * 0.7, padding: `${D.label * 0.6}px 0`,
          borderTop: `1px solid ${T.line}`,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: D.label * 0.4,
            fontSize: D.label, color: T.textMuted }}>
            <SectionIcon name="clock" color={T.textMuted} size={Math.round(D.label * 1.1)} />
            Duration
          </div>
          <div style={{ fontSize: D.h3, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>{formatDuration(s.out.durationH)}</div>
        </div>
      </Card>

      {/* Summary */}
      <Card style={{ background: T.surface }}>
        <SectionHeader icon="euro" label="Charging cost" accent={A.base} D={D} T={T} />
        <SummaryRow label="Energy consumed" value={`${s.out.energyKwh.toFixed(2)} kWh`} D={D} T={T} />
        <SummaryRow label="Added to battery" value={`${s.out.addedKwh.toFixed(2)} kWh`} D={D} T={T} />
        <SummaryRow label="Lost to heat"     value={`${s.out.lostKwh.toFixed(2)} kWh`} D={D} T={T} />
        <SummaryRow label="Spot price" value={<span style={{ display: 'inline-flex', alignItems: 'center', gap: D.label * 0.35 }}>
          {s.spotPrice.toFixed(2)} c/kWh
          <span style={{ width: D.label * 0.7, height: D.label * 0.7, background: T.successInk, borderRadius: 2, display: 'inline-block' }} />
        </span>} D={D} T={T} valueColor={T.successInk} />
        <div style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
          marginTop: D.gap * 0.5, paddingTop: D.gap * 0.5, borderTop: `1px solid ${T.lineBold}`,
        }}>
          <div style={{ fontSize: D.h3, fontWeight: 700 }}>Total</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: D.label * 0.5,
            fontSize: D.h2, fontWeight: 700, color: T.successInk, fontVariantNumeric: 'tabular-nums' }}>
            {s.out.totalCost.toFixed(2)} €
            <span style={{ width: D.label * 0.7, height: D.label * 0.7, background: T.successInk, borderRadius: 2, display: 'inline-block' }} />
          </div>
        </div>
      </Card>

      {sheet === 'vehicle' && <VehicleSheet s={s} D={D} T={T} A={A} dark={dark} onClose={() => setSheet(null)} />}
    </div>
  );
}

function TimeField({ label, hour, onChange, D, T, A, editable, dim }) {
  const text = formatTime(hour % 24);
  const dayOff = Math.floor(hour / 24);
  return (
    <div style={{ opacity: dim ? 0.5 : 1 }}>
      <div style={{ fontSize: D.label, color: T.textMuted, marginBottom: 4 }}>{label}</div>
      <div style={{
        background: editable ? 'transparent' : T.track,
        border: `1px solid ${editable ? A.base : T.line}`,
        borderRadius: D.radius * 0.7, padding: `${D.label * 0.5}px ${D.label * 0.8}px`,
        display: 'flex', alignItems: 'baseline', gap: 6,
      }}>
        <div style={{ fontSize: D.h3, fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>{text}</div>
        {dayOff > 0 && <div style={{ fontSize: D.label, color: T.textMuted }}>+{dayOff}d</div>}
      </div>
    </div>
  );
}

function SummaryRow({ label, value, D, T, valueColor }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
      padding: `${D.label * 0.4}px 0`, fontSize: D.base }}>
      <div style={{ color: T.textMuted }}>{label}</div>
      <div style={{ fontWeight: 600, color: valueColor || T.text, fontVariantNumeric: 'tabular-nums' }}>{value}</div>
    </div>
  );
}

// Bottom sheet — tap anywhere outside to dismiss; lists vehicles with per-row
// accent dot, battery, and a check on the selected one.
function VehicleSheet({ s, D, T, A, dark, onClose }) {
  return (
    <div onClick={onClose}
      style={{
        position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.5)',
        display: 'flex', alignItems: 'flex-end', justifyContent: 'center',
        zIndex: 50,
      }}>
      <div onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%', background: T.surface,
          borderTopLeftRadius: D.radius * 1.5, borderTopRightRadius: D.radius * 1.5,
          padding: D.pad, maxHeight: '80%', overflow: 'auto',
          boxShadow: '0 -8px 40px rgba(0,0,0,0.3)',
        }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: D.gap }}>
          <div style={{ fontSize: D.h3, fontWeight: 700 }}>Choose vehicle</div>
          <button onClick={onClose} style={{
            background: 'none', border: 'none', fontSize: D.h3, color: T.textMuted, cursor: 'pointer',
          }}>✕</button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: D.gap * 0.4 }}>
          {VEHICLES.map((v) => {
            const active = v.id === s.vehicleId;
            const ramp = accentRamp(v.accent, dark);
            return (
              <button key={v.id} onClick={() => { s.setVehicleId(v.id); onClose(); }}
                style={{
                  display: 'flex', alignItems: 'center', gap: D.gap * 0.7,
                  padding: D.gap * 0.5, borderRadius: D.radius,
                  background: active ? ramp.soft : 'transparent',
                  border: `1px solid ${active ? ramp.base : T.line}`,
                  color: T.text, cursor: 'pointer', textAlign: 'left',
                }}>
                <CarSilhouette sketch={v.sketch} color={ramp.base} width={D.h2 * 1.4} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: D.h3, fontWeight: 600 }}>{v.name}</div>
                  <div style={{ fontSize: D.label, color: T.textMuted }}>{v.trim} · {v.battery} kWh · {v.consumption} kWh/100km</div>
                </div>
                {active && <div style={{ color: ramp.base, fontSize: D.h3 }}>✓</div>}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

window.ChargingV1 = ChargingV1;
window.VehicleSheet = VehicleSheet;
