// charging-core.jsx — Shared state, calculations, and primitives for the
// Liukuri charging-view redesign. All three variants pull from here.

// ── Vehicle catalogue ───────────────────────────────────────────────
// Each vehicle gets an accent (per-vehicle color picked from the brand
// silhouette) — used everywhere as the active color in dark + light themes.
// Image is a simple SVG silhouette so we can recolor + scale freely without
// real product photos.
const VEHICLES = [
  { id: 'polestar2-lr', name: 'Polestar 2', trim: 'Long Range', battery: 78, consumption: 17.5, accent: { h: 28, c: 0.18 }, sketch: 'fastback' },
  { id: 'tesla-m3-lr',  name: 'Tesla Model 3', trim: 'Long Range', battery: 75, consumption: 14.5, accent: { h: 250, c: 0.16 }, sketch: 'sedan' },
  { id: 'ioniq5',       name: 'Hyundai Ioniq 5', trim: '77 kWh AWD', battery: 77, consumption: 18.0, accent: { h: 195, c: 0.14 }, sketch: 'crossover' },
  { id: 'id4',          name: 'Volkswagen ID.4', trim: 'Pro', battery: 77, consumption: 18.5, accent: { h: 145, c: 0.13 }, sketch: 'suv' },
  { id: 'zoe',          name: 'Renault Zoe', trim: 'R135', battery: 52, consumption: 16.0, accent: { h: 90,  c: 0.16 }, sketch: 'hatch' },
  { id: 'enyaq',        name: 'Škoda Enyaq', trim: '85', battery: 82, consumption: 18.2, accent: { h: 165, c: 0.14 }, sketch: 'suv' },
];
const VEHICLE_BY_ID = Object.fromEntries(VEHICLES.map((v) => [v.id, v]));

// ── Color helpers ───────────────────────────────────────────────────
// Build the full accent ramp from the vehicle's hue/chroma so foreground
// surfaces, hover, and disabled states stay harmonious without per-vehicle
// hand-tuning. oklch() is supported by every modern engine we target.
function accentRamp(accent, dark) {
  const { h, c } = accent;
  if (dark) {
    return {
      base:  `oklch(0.72 ${c} ${h})`,
      bold:  `oklch(0.78 ${c} ${h})`,
      soft:  `oklch(0.72 ${c} ${h} / 0.18)`,
      ghost: `oklch(0.72 ${c} ${h} / 0.08)`,
      ink:   '#0a0a0a',
    };
  }
  return {
    base:  `oklch(0.55 ${c} ${h})`,
    bold:  `oklch(0.48 ${c} ${h})`,
    soft:  `oklch(0.55 ${c} ${h} / 0.14)`,
    ghost: `oklch(0.55 ${c} ${h} / 0.06)`,
    ink:   '#fff',
  };
}

// Theme tokens used by every variant. Keep this small + explicit; each
// variant overlays its own visual flourishes on top.
function theme(dark) {
  return dark ? {
    bg:        '#0e0f12',
    surface:   '#1a1c20',
    surface2:  '#22252a',
    line:      'rgba(255,255,255,0.08)',
    lineBold:  'rgba(255,255,255,0.16)',
    text:      '#f4f4f5',
    textMuted: 'rgba(244,244,245,0.62)',
    textFaint: 'rgba(244,244,245,0.4)',
    track:     'rgba(255,255,255,0.10)',
    danger:    '#e76f51',
    success:   'oklch(0.78 0.15 145)',
    successInk:'oklch(0.85 0.18 145)',
  } : {
    bg:        '#f6f6f4',
    surface:   '#ffffff',
    surface2:  '#fafaf8',
    line:      'rgba(15,15,15,0.08)',
    lineBold:  'rgba(15,15,15,0.16)',
    text:      '#171717',
    textMuted: 'rgba(23,23,23,0.62)',
    textFaint: 'rgba(23,23,23,0.4)',
    track:     'rgba(15,15,15,0.08)',
    danger:    '#c0392b',
    success:   'oklch(0.5 0.14 145)',
    successInk:'oklch(0.42 0.16 145)',
  };
}

// Density preset — the same prototype renders at "in-car" (huge), "tablet",
// "desktop", "mobile" with proportional type/spacing. All numbers are in
// CSS px in the prototype's own coordinate system; the canvas counter-scales
// so on-screen pixels stay consistent.
const DENSITY = {
  // Mobile matches the original auto.liukuri.fi web sizing (smaller numerics).
  mobile:  { base: 14, h1: 28, h2: 22, h3: 17, label: 12, pad: 16, gap: 14, radius: 12, control: 40 },
  // Tablet, desktop and in-car all share the in-car scale so the prototype
  // is one-handed-tap friendly on the Polestar 2 (1152×1536) and reads at the
  // same physical size on desktop monitors.
  tablet:  { base: 24, h1: 86, h2: 56, h3: 36, label: 22, pad: 36, gap: 28, radius: 18, control: 88 },
  desktop: { base: 24, h1: 86, h2: 56, h3: 36, label: 22, pad: 36, gap: 28, radius: 18, control: 88 },
  incar:   { base: 24, h1: 86, h2: 56, h3: 36, label: 22, pad: 36, gap: 28, radius: 18, control: 88 },
};

// ── Charging math ───────────────────────────────────────────────────
// AC charging — assume 3-phase 230V on 11kW wallboxes (16A nominal).
// efficiency captures heat loss through the on-board charger.
const AC_VOLTS = 230;
const AC_PHASES = 3;
const AC_EFFICIENCY = 0.90;

function powerForAmps(amps, phases = AC_PHASES, voltage = AC_VOLTS) {
  return (voltage * amps * phases) / 1000;
}

function calculate({ vehicle, currentPct, targetPct, amps, phases = AC_PHASES, voltage = AC_VOLTS, lossPct = 10, spotPriceCkwh = 3.98 }) {
  const battery = vehicle.battery;
  const efficiency = Math.max(0.01, 1 - lossPct / 100);
  const addedKwh   = Math.max(0, (targetPct - currentPct) / 100) * battery;
  const power      = powerForAmps(amps, phases, voltage);
  const energyKwh  = addedKwh / efficiency;
  const lostKwh    = energyKwh - addedKwh;
  const durationH  = power > 0 ? energyKwh / power : 0;
  const rangeKm    = (addedKwh / vehicle.consumption) * 100;
  const totalRangeKm = (battery / vehicle.consumption) * 100;
  const currentKm  = (battery * currentPct / 100) / vehicle.consumption * 100;
  const targetKm   = (battery * targetPct  / 100) / vehicle.consumption * 100;
  const totalCost  = (energyKwh * spotPriceCkwh) / 100; // ¢/kWh → €
  return {
    addedKwh, energyKwh, lostKwh, durationH, rangeKm,
    totalRangeKm, currentKm, targetKm, power, totalCost, spotPriceCkwh,
  };
}

function formatDuration(h) {
  const total = Math.max(0, Math.round(h * 60));
  const hh = Math.floor(total / 60);
  const mm = total % 60;
  return `${hh}h ${mm.toString().padStart(2, '0')}min`;
}
function formatTime(h) {
  const total = Math.round(h * 60);
  const hh = Math.floor(total / 60) % 24;
  const mm = total % 60;
  return `${hh.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}`;
}
function addHours(startHour, hours) {
  // returns { hour, dayOffset }
  const total = startHour + hours;
  return { hour: ((total % 24) + 24) % 24, dayOffset: Math.floor(total / 24) };
}

// ── Shared primitives ───────────────────────────────────────────────
// CarSilhouette — pure-svg generic EV silhouette colored from accent.
// We keep one SVG family and skew proportions slightly per "sketch" type
// so every vehicle has visual identity without a sprite atlas.
function CarSilhouette({ sketch = 'fastback', color = 'currentColor', width = 120, opacity = 1 }) {
  // Same path family, tuned per body type via path stroke weights & roof line.
  const paths = {
    fastback:  'M6 36 L18 18 Q24 14 36 12 L78 12 Q92 16 100 22 L122 30 Q128 32 128 38 L128 44 L8 44 Q4 40 6 36 Z',
    sedan:     'M6 36 L20 22 Q26 18 38 16 L72 16 Q86 18 96 24 L122 32 Q128 34 128 40 L128 44 L8 44 Q4 40 6 36 Z',
    crossover: 'M6 34 L18 16 Q26 12 38 10 L80 10 Q94 14 102 20 L124 28 Q130 30 130 36 L130 44 L8 44 Q4 40 6 34 Z',
    suv:       'M6 30 L18 12 Q28 8 40 6 L82 6 Q96 10 104 16 L126 24 Q132 26 132 32 L132 44 L8 44 Q4 38 6 30 Z',
    hatch:     'M6 36 L20 18 Q28 14 40 12 L74 12 Q86 18 92 26 L116 32 Q124 34 124 40 L124 44 L8 44 Q4 40 6 36 Z',
  };
  const d = paths[sketch] || paths.fastback;
  const ratio = 50 / 138;
  const h = Math.round(width * ratio);
  return (
    <svg viewBox="0 0 138 50" width={width} height={h} style={{ display: 'block', opacity }} aria-hidden="true">
      <path d={d} fill={color} />
      <circle cx="32" cy="44" r="7" fill={color} />
      <circle cx="32" cy="44" r="3" fill="rgba(0,0,0,0.35)" />
      <circle cx="100" cy="44" r="7" fill={color} />
      <circle cx="100" cy="44" r="3" fill="rgba(0,0,0,0.35)" />
      {/* window glass */}
      <path d="M28 18 L40 14 L74 14 L88 22 L84 26 L34 26 Z" fill="rgba(0,0,0,0.18)" />
    </svg>
  );
}

// DualSlider — two thumbs on a single track, used for current → target.
// Renders an accent fill between the thumbs, ghost fills outside, and
// stays pleasant at any size (touch-target scales with `size` prop).
function DualSlider({ low, high, min = 0, max = 100, onChange, accent, track, height = 12, thumbSize = 28, disabled }) {
  const ref = React.useRef(null);
  const drag = React.useRef(null);
  const pct = (v) => ((v - min) / (max - min)) * 100;

  const handleDown = (which) => (e) => {
    if (disabled) return;
    e.preventDefault();
    drag.current = which;
    const move = (ev) => {
      const r = ref.current.getBoundingClientRect();
      const x = (ev.clientX ?? ev.touches?.[0]?.clientX) - r.left;
      const v = Math.round(min + Math.max(0, Math.min(1, x / r.width)) * (max - min));
      if (drag.current === 'low')  onChange(Math.min(v, high - 1), high);
      if (drag.current === 'high') onChange(low, Math.max(v, low + 1));
    };
    const up = () => {
      drag.current = null;
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', up);
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
  };

  // Click on the track moves the nearer thumb so the user doesn't have to
  // grab the small handle to make a coarse adjustment.
  const onTrackDown = (e) => {
    if (disabled) return;
    const r = ref.current.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width;
    const v = Math.round(min + x * (max - min));
    const which = Math.abs(v - low) < Math.abs(v - high) ? 'low' : 'high';
    if (which === 'low')  onChange(Math.min(v, high - 1), high);
    else                  onChange(low, Math.max(v, low + 1));
    drag.current = which;
    handleDown(which)(e);
  };

  return (
    <div ref={ref} onPointerDown={onTrackDown}
      style={{ position: 'relative', height: thumbSize, cursor: disabled ? 'default' : 'pointer', touchAction: 'none' }}>
      <div style={{ position: 'absolute', left: 0, right: 0, top: '50%', transform: 'translateY(-50%)',
        height, borderRadius: height, background: track }} />
      <div style={{ position: 'absolute', left: `${pct(low)}%`, right: `${100 - pct(high)}%`, top: '50%', transform: 'translateY(-50%)',
        height, borderRadius: height, background: accent }} />
      {[['low', low], ['high', high]].map(([w, v]) => (
        <button key={w} onPointerDown={handleDown(w)} aria-label={`${w} ${v}%`}
          style={{
            position: 'absolute', left: `calc(${pct(v)}% - ${thumbSize / 2}px)`, top: 0,
            width: thumbSize, height: thumbSize, borderRadius: '50%', padding: 0,
            background: '#fff', border: `2px solid ${accent}`,
            boxShadow: '0 1px 4px rgba(0,0,0,0.18)', cursor: disabled ? 'default' : 'grab',
            touchAction: 'none',
          }} />
      ))}
    </div>
  );
}

// SingleSlider — like DualSlider but with one thumb. Keeps the same look so
// the charging-speed control rhymes visually with the SoC slider.
function SingleSlider({ value, min = 6, max = 32, step = 1, onChange, accent, track, height = 10, thumbSize = 26 }) {
  const ref = React.useRef(null);
  const onDown = (e) => {
    e.preventDefault();
    const set = (ev) => {
      const r = ref.current.getBoundingClientRect();
      const x = (ev.clientX ?? ev.touches?.[0]?.clientX) - r.left;
      const v = Math.round((min + Math.max(0, Math.min(1, x / r.width)) * (max - min)) / step) * step;
      onChange(Math.max(min, Math.min(max, v)));
    };
    set(e);
    const move = set;
    const up = () => { window.removeEventListener('pointermove', move); window.removeEventListener('pointerup', up); };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
  };
  const pct = ((value - min) / (max - min)) * 100;
  return (
    <div ref={ref} onPointerDown={onDown}
      style={{ position: 'relative', height: thumbSize, touchAction: 'none', cursor: 'pointer' }}>
      <div style={{ position: 'absolute', left: 0, right: 0, top: '50%', transform: 'translateY(-50%)',
        height, borderRadius: height, background: track }} />
      <div style={{ position: 'absolute', left: 0, width: `${pct}%`, top: '50%', transform: 'translateY(-50%)',
        height, borderRadius: height, background: accent }} />
      <div style={{ position: 'absolute', left: `calc(${pct}% - ${thumbSize / 2}px)`, top: 0,
        width: thumbSize, height: thumbSize, borderRadius: '50%',
        background: '#fff', border: `2px solid ${accent}`,
        boxShadow: '0 1px 4px rgba(0,0,0,0.18)' }} />
    </div>
  );
}

// useChargingState — single source of truth shared across variants. Holds the
// inputs (vehicle, current/target SoC, amps, phases, schedule) and exposes
// `out` (calculated values) plus actions. Every variant consumes one of
// these so the underlying behaviour is identical across designs.
function useChargingState() {
  const [vehicleId, setVehicleId] = React.useState('polestar2-lr');
  const [currentPct, setCurrentPct] = React.useState(29);
  const [targetPct, setTargetPct] = React.useState(80);
  const [amps, setAmps] = React.useState(16);
  const [phases, setPhases] = React.useState(3);
  const [voltage, setVoltage] = React.useState(230);
  const [lossPct, setLossPct] = React.useState(10);
  const [startHour, setStartHour] = React.useState(9);
  const [calcMode, setCalcMode] = React.useState('end'); // 'end' (start known) or 'start' (end known)
  const [endHour, setEndHour] = React.useState(13);
  const [spotPrice, setSpotPrice] = React.useState(3.98);

  const vehicle = VEHICLE_BY_ID[vehicleId] || VEHICLES[0];
  const out = React.useMemo(() => calculate({
    vehicle, currentPct, targetPct, amps, phases, voltage, lossPct, spotPriceCkwh: spotPrice,
  }), [vehicle, currentPct, targetPct, amps, phases, voltage, lossPct, spotPrice]);

  // Schedule sync: when calcMode is 'end', endHour is derived; when 'start',
  // startHour is derived. We keep both in state to allow the user to flip the
  // mode without losing input — the inactive one re-derives when mode flips.
  React.useEffect(() => {
    if (calcMode === 'end') {
      const e = addHours(startHour, out.durationH);
      setEndHour(e.hour + e.dayOffset * 24);
    } else {
      const s = addHours(endHour, -out.durationH);
      setStartHour(s.hour + s.dayOffset * 24);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [out.durationH, calcMode]);

  const setSoc = (lo, hi) => { setCurrentPct(lo); setTargetPct(hi); };

  return {
    vehicle, vehicleId, setVehicleId,
    currentPct, targetPct, setSoc,
    amps, setAmps, phases, setPhases, voltage, setVoltage, lossPct, setLossPct,
    startHour, setStartHour, endHour, setEndHour,
    calcMode, setCalcMode,
    spotPrice, setSpotPrice,
    out,
  };
}

// AdvancedSettings — disclosure block with Phases / Voltage / Loss% steppers.
// Used by every variant under the Charging-speed slider.
function AdvancedSettings({ s, D, T, accent }) {
  const [open, setOpen] = React.useState(false);
  const Stepper = ({ label, value, suffix, onDec, onInc }) => (
    <div style={{ flex: 1, minWidth: 0 }}>
      <div style={{ fontSize: D.label, color: T.textMuted, marginBottom: D.label * 0.3 }}>{label}</div>
      <div style={{ display: 'flex', alignItems: 'stretch', background: T.surface2,
        border: `1px solid ${T.line}`, borderRadius: D.radius * 0.5, overflow: 'hidden' }}>
        <button onClick={onDec} style={{ flex: '0 0 auto', padding: `0 ${D.label * 0.7}px`,
          background: 'transparent', border: 'none', color: T.text, fontSize: D.h3 * 0.7,
          cursor: 'pointer', fontFamily: 'inherit' }}>−</button>
        <div style={{ flex: 1, textAlign: 'center', fontSize: D.base, fontWeight: 600,
          color: T.text, padding: `${D.label * 0.4}px 0`, fontVariantNumeric: 'tabular-nums' }}>
          {value}{suffix && <span style={{ color: T.textMuted, fontWeight: 400, marginLeft: 4 }}>{suffix}</span>}
        </div>
        <button onClick={onInc} style={{ flex: '0 0 auto', padding: `0 ${D.label * 0.7}px`,
          background: 'transparent', border: 'none', color: T.text, fontSize: D.h3 * 0.7,
          cursor: 'pointer', fontFamily: 'inherit' }}>+</button>
      </div>
    </div>
  );
  return (
    <div>
      <button onClick={() => setOpen(!open)}
        style={{ background: 'transparent', border: 'none', color: accent || T.textMuted,
          fontSize: D.label, padding: `${D.label * 0.3}px 0`, cursor: 'pointer',
          fontFamily: 'inherit', display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ display: 'inline-block', transition: 'transform 0.2s',
          transform: open ? 'rotate(90deg)' : 'rotate(0deg)' }}>›</span>
        Advanced
      </button>
      {open && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: D.gap * 0.5, marginTop: D.gap * 0.5,
          paddingTop: D.gap * 0.4, borderTop: `1px dashed ${T.line}` }}>
          <Stepper label="Phases" value={s.phases} suffix="∅"
            onDec={() => s.setPhases(Math.max(1, s.phases - 1))}
            onInc={() => s.setPhases(Math.min(3, s.phases + 1))} />
          <Stepper label="Voltage" value={s.voltage} suffix="V"
            onDec={() => s.setVoltage(Math.max(100, s.voltage - 5))}
            onInc={() => s.setVoltage(Math.min(450, s.voltage + 5))} />
          <Stepper label="Charging Loss" value={s.lossPct} suffix="%"
            onDec={() => s.setLossPct(Math.max(0, s.lossPct - 1))}
            onInc={() => s.setLossPct(Math.min(30, s.lossPct + 1))} />
        </div>
      )}
    </div>
  );
}

Object.assign(window, {
  VEHICLES, VEHICLE_BY_ID, accentRamp, theme, DENSITY,
  calculate, formatDuration, formatTime, addHours, powerForAmps,
  CarSilhouette, DualSlider, SingleSlider, useChargingState,
  AdvancedSettings,
});
