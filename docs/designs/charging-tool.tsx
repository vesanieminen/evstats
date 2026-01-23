import { useState } from 'react';
import { ChevronDown, ChevronUp, Zap, Clock, Battery, Settings, Leaf } from 'lucide-react';

const CarSVG = ({ type, color }) => {
  const cars = {
    sedan: (
      <svg viewBox="0 0 200 100" className="w-full h-full" style={{filter:'drop-shadow(0 4px 6px rgba(0,0,0,0.1))'}}>
        <defs>
          <linearGradient id="s-body" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stopColor={color}/><stop offset="100%" stopColor={color} stopOpacity="0.7"/></linearGradient>
          <linearGradient id="s-glass" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stopColor="#e0f4ff"/><stop offset="100%" stopColor="#a8d8ea"/></linearGradient>
        </defs>
        <ellipse cx="100" cy="88" rx="90" ry="5" fill="#94a3b8" opacity="0.2"/>
        <path d="M12,72 L20,72 L32,52 L62,35 L88,28 L150,28 L178,38 L192,58 L198,72 L198,80 L10,80 Z" fill="url(#s-body)"/>
        <path d="M68,35 L86,24 L115,24 L115,40 L70,40 Z" fill="url(#s-glass)"/><path d="M120,40 L120,24 L146,24 L168,38 L168,40 Z" fill="url(#s-glass)"/>
        <ellipse cx="58" cy="80" rx="17" ry="17" fill="#334155"/><ellipse cx="58" cy="80" rx="13" ry="13" fill="#475569"/><ellipse cx="58" cy="80" rx="7" ry="7" fill="#64748b"/>
        <ellipse cx="158" cy="80" rx="17" ry="17" fill="#334155"/><ellipse cx="158" cy="80" rx="13" ry="13" fill="#475569"/><ellipse cx="158" cy="80" rx="7" ry="7" fill="#64748b"/>
      </svg>
    ),
    suv: (
      <svg viewBox="0 0 200 100" className="w-full h-full" style={{filter:'drop-shadow(0 4px 6px rgba(0,0,0,0.1))'}}>
        <defs><linearGradient id="u-body" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stopColor={color}/><stop offset="100%" stopColor={color} stopOpacity="0.7"/></linearGradient></defs>
        <ellipse cx="100" cy="88" rx="92" ry="5" fill="#94a3b8" opacity="0.2"/>
        <path d="M8,68 L15,68 L25,45 L52,25 L78,18 L155,18 L180,28 L195,52 L200,68 L202,78 L5,78 Z" fill="url(#u-body)"/>
        <rect x="58" y="14" width="95" height="3" rx="1.5" fill="#94a3b8"/>
        <path d="M55,28 L72,18 L105,18 L105,38 L58,38 Z" fill="url(#s-glass)"/><path d="M110,38 L110,18 L152,18 L175,28 L175,38 Z" fill="url(#s-glass)"/>
        <ellipse cx="55" cy="78" rx="19" ry="19" fill="#334155"/><ellipse cx="55" cy="78" rx="15" ry="15" fill="#475569"/><ellipse cx="55" cy="78" rx="9" ry="9" fill="#64748b"/>
        <ellipse cx="158" cy="78" rx="19" ry="19" fill="#334155"/><ellipse cx="158" cy="78" rx="15" ry="15" fill="#475569"/><ellipse cx="158" cy="78" rx="9" ry="9" fill="#64748b"/>
      </svg>
    ),
    hatchback: (
      <svg viewBox="0 0 200 100" className="w-full h-full" style={{filter:'drop-shadow(0 4px 6px rgba(0,0,0,0.1))'}}>
        <defs><linearGradient id="h-body" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stopColor={color}/><stop offset="100%" stopColor={color} stopOpacity="0.7"/></linearGradient></defs>
        <ellipse cx="100" cy="85" rx="85" ry="5" fill="#94a3b8" opacity="0.2"/>
        <path d="M18,72 L25,72 L38,50 L65,32 L90,26 L160,26 L182,38 L190,55 L195,72 L198,78 L15,78 Z" fill="url(#h-body)"/>
        <path d="M68,32 L85,26 L112,26 L112,42 L70,42 Z" fill="url(#s-glass)"/><path d="M117,42 L117,26 L160,26 L178,38 L178,42 Z" fill="url(#s-glass)"/>
        <path d="M25,55 L38,38 L62,38 L62,55 Z" fill="url(#s-glass)"/>
        <ellipse cx="62" cy="78" rx="16" ry="16" fill="#334155"/><ellipse cx="62" cy="78" rx="12" ry="12" fill="#475569"/>
        <ellipse cx="155" cy="78" rx="16" ry="16" fill="#334155"/><ellipse cx="155" cy="78" rx="12" ry="12" fill="#475569"/>
      </svg>
    ),
    sports: (
      <svg viewBox="0 0 200 100" className="w-full h-full" style={{filter:'drop-shadow(0 4px 6px rgba(0,0,0,0.1))'}}>
        <defs><linearGradient id="sp-body" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stopColor={color}/><stop offset="100%" stopColor={color} stopOpacity="0.7"/></linearGradient></defs>
        <ellipse cx="100" cy="82" rx="92" ry="4" fill="#94a3b8" opacity="0.2"/>
        <path d="M8,68 L18,62 L35,52 L72,42 L98,38 L158,38 L185,45 L198,58 L202,68 L202,75 L5,75 Z" fill="url(#sp-body)"/>
        <path d="M92,38 L102,28 L142,28 L158,38 Z" fill={color} opacity="0.6"/>
        <path d="M95,40 L104,30 L128,30 L128,40 Z" fill="url(#s-glass)"/><path d="M132,40 L132,30 L148,30 L158,40 Z" fill="url(#s-glass)"/>
        <ellipse cx="58" cy="72" rx="16" ry="16" fill="#334155"/><ellipse cx="58" cy="72" rx="12" ry="12" fill="#475569"/>
        <ellipse cx="158" cy="72" rx="16" ry="16" fill="#334155"/><ellipse cx="158" cy="72" rx="12" ry="12" fill="#475569"/>
      </svg>
    ),
    truck: (
      <svg viewBox="0 0 240 100" className="w-full h-full" style={{filter:'drop-shadow(0 4px 6px rgba(0,0,0,0.1))'}}>
        <defs><linearGradient id="t-body" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stopColor={color}/><stop offset="100%" stopColor={color} stopOpacity="0.7"/></linearGradient></defs>
        <ellipse cx="120" cy="88" rx="110" ry="5" fill="#94a3b8" opacity="0.2"/>
        <path d="M5,48 L5,78 L125,78 L125,32 L105,32 Z" fill={color} opacity="0.5"/>
        <rect x="5" y="30" width="120" height="3" fill={color} opacity="0.7"/>
        <path d="M125,78 L125,28 L148,18 L178,15 L212,15 L232,25 L240,48 L245,78 Z" fill="url(#t-body)"/>
        <path d="M130,28 L150,18 L182,18 L182,38 L132,38 Z" fill="url(#s-glass)"/><path d="M187,38 L187,18 L210,18 L228,28 L228,38 Z" fill="url(#s-glass)"/>
        <ellipse cx="58" cy="78" rx="20" ry="20" fill="#334155"/><ellipse cx="58" cy="78" rx="16" ry="16" fill="#475569"/>
        <ellipse cx="195" cy="78" rx="20" ry="20" fill="#334155"/><ellipse cx="195" cy="78" rx="16" ry="16" fill="#475569"/>
      </svg>
    ),
  };
  return cars[type] || cars.sedan;
};

const evModels = [
  { name: 'Tesla Model 3', capacity: 75, type: 'sedan', color: '#0ea5e9' },
  { name: 'Tesla Model Y', capacity: 75, type: 'suv', color: '#f8fafc' },
  { name: 'BMW iX', capacity: 112, type: 'suv', color: '#1e3a5f' },
  { name: 'Porsche Taycan', capacity: 93, type: 'sports', color: '#0f766e' },
  { name: 'VW ID.3', capacity: 58, type: 'hatchback', color: '#f97316' },
  { name: 'Rivian R1T', capacity: 135, type: 'truck', color: '#365314' },
  { name: 'Polestar 2', capacity: 78, type: 'sedan', color: '#fbbf24' },
  { name: 'Hyundai Ioniq 5', capacity: 77, type: 'suv', color: '#6366f1' },
];

export default function NordicLightTheme() {
  const [model, setModel] = useState(evModels[0]);
  const [currentSOC, setCurrentSOC] = useState(20);
  const [targetSOC, setTargetSOC] = useState(80);
  const [speed, setSpeed] = useState(16);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [phases, setPhases] = useState(3);
  const [voltage, setVoltage] = useState(230);
  const [startTime, setStartTime] = useState('11:00');
  const [showPicker, setShowPicker] = useState(false);

  const kW = (speed * voltage * phases) / 1000;
  const loss = kW > 11 ? 0.08 : kW > 7 ? 0.10 : 0.12;
  const energy = model.capacity * (targetSOC - currentSOC) / 100;
  const hours = energy / (kW * (1 - loss));
  const h = Math.floor(hours), m = Math.floor((hours - h) * 60);
  const consumed = energy / (1 - loss);
  const cost = (consumed * 19.08 / 100).toFixed(2);
  const range = Math.round(energy * 5.5);
  
  const getEnd = () => {
    const [hr, mn] = startTime.split(':').map(Number);
    const end = hr * 60 + mn + hours * 60;
    return `${Math.floor(end / 60) % 24}:${String(Math.floor(end % 60)).padStart(2, '0')}`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-100 via-sky-50 to-white text-slate-800 p-4 max-w-md mx-auto">
      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center gap-2">
          <Leaf className="w-5 h-5 text-emerald-500"/>
          <h1 className="text-2xl font-light text-slate-700">Charging</h1>
        </div>
        <div className="text-xs text-sky-600 bg-sky-100 px-3 py-1 rounded-full">Nordic Light</div>
      </div>

      {/* Car Card */}
      <div onClick={() => setShowPicker(!showPicker)} className="relative bg-white/80 backdrop-blur border border-slate-200 shadow-sm rounded-3xl p-5 mb-4 cursor-pointer hover:border-sky-400 transition-all">
        <div className="h-24 mb-3">{CarSVG({ type: model.type, color: model.color })}</div>
        <div className="flex justify-between items-end">
          <div>
            <p className="font-medium text-lg text-slate-700">{model.name}</p>
            <p className="text-sm text-slate-400">{model.capacity} kWh</p>
          </div>
          <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform ${showPicker ? 'rotate-180' : ''}`}/>
        </div>
        <div className="grid grid-cols-3 gap-4 mt-5 pt-4 border-t border-slate-100">
          <div className="text-center"><p className="text-2xl font-light text-sky-600">{currentSOC}%</p><p className="text-xs text-slate-400">Current</p></div>
          <div className="text-center"><p className="text-2xl font-light text-emerald-600">{targetSOC}%</p><p className="text-xs text-slate-400">Target</p></div>
          <div className="text-center"><p className="text-2xl font-light text-slate-600">{range}km</p><p className="text-xs text-slate-400">Range</p></div>
        </div>

        {showPicker && (
          <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-slate-200 shadow-lg rounded-2xl overflow-hidden z-20 max-h-64 overflow-y-auto">
            {evModels.map((m, i) => (
              <div key={i} onClick={(e) => { e.stopPropagation(); setModel(m); setShowPicker(false); }} className={`flex items-center gap-3 p-3 hover:bg-sky-50 ${model.name === m.name ? 'bg-sky-100' : ''}`}>
                <div className="w-20 h-10">{CarSVG({ type: m.type, color: m.color })}</div>
                <div><p className="text-sm font-medium text-slate-700">{m.name}</p><p className="text-xs text-slate-400">{m.capacity} kWh</p></div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* SOC */}
      <div className="bg-white/80 backdrop-blur border border-slate-200 shadow-sm rounded-3xl p-5 mb-4">
        <div className="flex items-center gap-2 text-slate-500 mb-4"><Battery className="w-4 h-4"/>Charge Level</div>
        <div className="relative h-12 mb-3">
          <div className="absolute inset-0 bg-slate-200 rounded-full overflow-hidden">
            <div className="absolute h-full bg-sky-200" style={{ width: `${currentSOC}%` }}/>
            <div className="absolute h-full bg-gradient-to-r from-sky-500 to-sky-400" style={{ left: `${currentSOC}%`, width: `${targetSOC - currentSOC}%` }}/>
          </div>
          <input type="range" min="5" max="95" value={currentSOC} onChange={e => setCurrentSOC(Math.min(+e.target.value, targetSOC - 5))} className="absolute inset-0 w-full opacity-0 cursor-pointer"/>
          <input type="range" min="5" max="100" value={targetSOC} onChange={e => setTargetSOC(Math.max(+e.target.value, currentSOC + 5))} className="absolute inset-0 w-full opacity-0 cursor-pointer"/>
          <div className="absolute top-1/2 bg-sky-500 text-white text-xs font-bold px-2 py-1 rounded-full shadow" style={{ left: `${currentSOC}%`, transform: 'translate(-50%, -50%)' }}>{currentSOC}%</div>
          <div className="absolute top-1/2 bg-emerald-500 text-white text-xs font-bold px-2 py-1 rounded-full shadow" style={{ left: `${targetSOC}%`, transform: 'translate(-50%, -50%)' }}>{targetSOC}%</div>
        </div>
      </div>

      {/* Speed */}
      <div className="bg-white/80 backdrop-blur border border-slate-200 shadow-sm rounded-3xl p-5 mb-4">
        <div className="flex justify-between items-center mb-4">
          <div className="flex items-center gap-2 text-slate-500"><Zap className="w-4 h-4"/>Speed</div>
          <span className="text-sky-600 font-medium">{kW.toFixed(1)} kW</span>
        </div>
        <input type="range" min="6" max="32" value={speed} onChange={e => setSpeed(+e.target.value)} className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-sky-500 mb-2"/>
        <div className="flex justify-between text-xs text-slate-400"><span>6A</span><span className="text-slate-600">{speed}A</span><span>32A</span></div>
        <button onClick={() => setShowAdvanced(!showAdvanced)} className="flex items-center gap-2 text-sm text-sky-600 mt-4"><Settings className="w-4 h-4"/>Advanced {showAdvanced ? <ChevronUp className="w-4 h-4"/> : <ChevronDown className="w-4 h-4"/>}</button>
        {showAdvanced && (
          <div className="mt-4 pt-4 border-t border-slate-100 space-y-4">
            <div><div className="flex justify-between text-sm text-slate-400 mb-2"><span>Phases</span><span>{phases}</span></div>
              <div className="flex gap-2">{[1,2,3].map(p => <button key={p} onClick={() => setPhases(p)} className={`flex-1 py-2 rounded-lg text-sm ${phases === p ? 'bg-sky-500 text-white' : 'bg-slate-100 text-slate-500'}`}>{p}P</button>)}</div>
            </div>
            <div><div className="flex justify-between text-sm text-slate-400 mb-2"><span>Voltage</span><span>{voltage}V</span></div>
              <input type="range" min="110" max="400" step="10" value={voltage} onChange={e => setVoltage(+e.target.value)} className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-sky-500"/>
            </div>
            <div className="flex justify-between text-sm"><span className="text-slate-400">Loss</span><span className="text-amber-600">{(loss*100).toFixed(0)}%</span></div>
          </div>
        )}
      </div>

      {/* Time */}
      <div className="bg-white/80 backdrop-blur border border-slate-200 shadow-sm rounded-3xl p-5 mb-4">
        <div className="flex items-center gap-2 text-slate-500 mb-4"><Clock className="w-4 h-4"/>Schedule</div>
        <div className="grid grid-cols-2 gap-4">
          <div><p className="text-xs text-slate-400 mb-1">Start</p><input type="time" value={startTime} onChange={e => setStartTime(e.target.value)} className="w-full bg-slate-100 border-none rounded-xl p-3 text-center text-lg text-slate-700"/></div>
          <div><p className="text-xs text-slate-400 mb-1">End</p><div className="bg-emerald-50 rounded-xl p-3 text-center text-lg text-emerald-600">{getEnd()}</div></div>
        </div>
        <div className="text-center mt-4"><p className="text-4xl font-light text-slate-700">{h}h {m}m</p><p className="text-sm text-slate-400">duration</p></div>
      </div>

      {/* Summary */}
      <div className="bg-gradient-to-br from-sky-100 to-emerald-50 border border-sky-200 rounded-3xl p-5">
        <h3 className="font-medium text-slate-700 mb-4">Summary</h3>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between"><span className="text-slate-500">To battery</span><span className="text-slate-700">{energy.toFixed(1)} kWh</span></div>
          <div className="flex justify-between"><span className="text-slate-500">Consumed</span><span className="text-slate-700">{consumed.toFixed(1)} kWh</span></div>
          <div className="flex justify-between"><span className="text-slate-500">Lost</span><span className="text-amber-600">{(consumed - energy).toFixed(1)} kWh</span></div>
          <div className="flex justify-between"><span className="text-slate-500">Spot price</span><span className="text-slate-700">19.08 c/kWh</span></div>
          <div className="border-t border-sky-200 pt-3 mt-3 flex justify-between items-center">
            <span className="text-slate-500">Cost</span><span className="text-3xl font-light text-emerald-600">{cost} €</span>
          </div>
        </div>
      </div>
      <p className="text-center text-xs text-slate-400 mt-6">aut.fi • Auto Liukuri</p>
    </div>
  );
}
  