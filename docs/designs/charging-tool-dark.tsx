import { useState } from 'react';
import { ChevronDown, ChevronUp, Zap, Clock, Battery, Settings, Palette } from 'lucide-react';

const carSVGs = {
  sedan: (color) => (
    <svg viewBox="0 0 200 80" className="w-full h-full">
      <defs>
        <linearGradient id={`car-grad-${color}`} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={color} stopOpacity="1"/>
          <stop offset="100%" stopColor={color} stopOpacity="0.7"/>
        </linearGradient>
      </defs>
      <path d="M20,55 L25,55 L30,45 L55,35 L75,30 L130,30 L155,35 L170,45 L180,55 L180,60 L20,60 Z" fill={`url(#car-grad-${color})`}/>
      <path d="M58,35 L72,22 L128,22 L148,35 Z" fill={color} opacity="0.3"/>
      <ellipse cx="50" cy="62" rx="15" ry="15" fill="#1a1a1a"/>
      <ellipse cx="50" cy="62" rx="10" ry="10" fill="#333"/>
      <ellipse cx="150" cy="62" rx="15" ry="15" fill="#1a1a1a"/>
      <ellipse cx="150" cy="62" rx="10" ry="10" fill="#333"/>
      <path d="M60,35 L75,24 L95,24 L95,35 Z" fill="#87CEEB" opacity="0.6"/>
      <path d="M100,35 L100,24 L125,24 L145,35 Z" fill="#87CEEB" opacity="0.6"/>
    </svg>
  ),
  suv: (color) => (
    <svg viewBox="0 0 200 80" className="w-full h-full">
      <defs>
        <linearGradient id={`suv-grad-${color}`} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={color} stopOpacity="1"/>
          <stop offset="100%" stopColor={color} stopOpacity="0.7"/>
        </linearGradient>
      </defs>
      <path d="M15,55 L20,55 L25,40 L45,25 L65,18 L140,18 L160,25 L175,40 L180,55 L185,60 L15,60 Z" fill={`url(#suv-grad-${color})`}/>
      <ellipse cx="50" cy="62" rx="16" ry="16" fill="#1a1a1a"/>
      <ellipse cx="50" cy="62" rx="11" ry="11" fill="#333"/>
      <ellipse cx="150" cy="62" rx="16" ry="16" fill="#1a1a1a"/>
      <ellipse cx="150" cy="62" rx="11" ry="11" fill="#333"/>
      <path d="M48,25 L62,18 L90,18 L90,32 L50,32 Z" fill="#87CEEB" opacity="0.6"/>
      <path d="M95,32 L95,18 L140,18 L155,25 L155,32 Z" fill="#87CEEB" opacity="0.6"/>
    </svg>
  ),
  hatchback: (color) => (
    <svg viewBox="0 0 200 80" className="w-full h-full">
      <defs>
        <linearGradient id={`hatch-grad-${color}`} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={color} stopOpacity="1"/>
          <stop offset="100%" stopColor={color} stopOpacity="0.7"/>
        </linearGradient>
      </defs>
      <path d="M25,55 L30,55 L35,42 L55,30 L75,25 L145,25 L165,35 L170,55 L175,60 L25,60 Z" fill={`url(#hatch-grad-${color})`}/>
      <ellipse cx="55" cy="62" rx="14" ry="14" fill="#1a1a1a"/>
      <ellipse cx="55" cy="62" rx="9" ry="9" fill="#333"/>
      <ellipse cx="145" cy="62" rx="14" ry="14" fill="#1a1a1a"/>
      <ellipse cx="145" cy="62" rx="9" ry="9" fill="#333"/>
      <path d="M58,30 L72,25 L95,25 L95,38 L60,38 Z" fill="#87CEEB" opacity="0.6"/>
      <path d="M100,38 L100,25 L145,25 L160,35 L160,38 Z" fill="#87CEEB" opacity="0.6"/>
    </svg>
  ),
  sports: (color) => (
    <svg viewBox="0 0 200 80" className="w-full h-full">
      <defs>
        <linearGradient id={`sport-grad-${color}`} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={color} stopOpacity="1"/>
          <stop offset="100%" stopColor={color} stopOpacity="0.7"/>
        </linearGradient>
      </defs>
      <path d="M15,55 L20,52 L30,48 L60,40 L80,38 L140,38 L165,42 L180,50 L185,55 L185,58 L15,58 Z" fill={`url(#sport-grad-${color})`}/>
      <path d="M75,38 L85,30 L125,30 L140,38 Z" fill={color} opacity="0.3"/>
      <ellipse cx="50" cy="58" rx="14" ry="14" fill="#1a1a1a"/>
      <ellipse cx="50" cy="58" rx="9" ry="9" fill="#333"/>
      <ellipse cx="150" cy="58" rx="14" ry="14" fill="#1a1a1a"/>
      <ellipse cx="150" cy="58" rx="9" ry="9" fill="#333"/>
      <path d="M78,38 L88,32 L115,32 L115,38 Z" fill="#87CEEB" opacity="0.6"/>
      <path d="M118,38 L118,32 L130,32 L138,38 Z" fill="#87CEEB" opacity="0.6"/>
    </svg>
  ),
  truck: (color) => (
    <svg viewBox="0 0 200 80" className="w-full h-full">
      <defs>
        <linearGradient id={`truck-grad-${color}`} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={color} stopOpacity="1"/>
          <stop offset="100%" stopColor={color} stopOpacity="0.7"/>
        </linearGradient>
      </defs>
      <path d="M10,55 L15,55 L20,35 L40,20 L60,15 L140,15 L155,18 L175,25 L185,55 L190,60 L10,60 Z" fill={`url(#truck-grad-${color})`}/>
      <rect x="95" y="25" width="90" height="30" fill={color} opacity="0.2"/>
      <ellipse cx="50" cy="62" rx="17" ry="17" fill="#1a1a1a"/>
      <ellipse cx="50" cy="62" rx="11" ry="11" fill="#333"/>
      <ellipse cx="155" cy="62" rx="17" ry="17" fill="#1a1a1a"/>
      <ellipse cx="155" cy="62" rx="11" ry="11" fill="#333"/>
      <path d="M42,20 L58,15 L88,15 L88,32 L45,32 Z" fill="#87CEEB" opacity="0.6"/>
    </svg>
  )
};

const evModels = [
  { name: 'Tesla Model 3 LR', capacity: 75, type: 'sedan', color: '#cc0000' },
  { name: 'Tesla Model Y LR', capacity: 75, type: 'suv', color: '#1a1a2e' },
  { name: 'Tesla Model S', capacity: 100, type: 'sedan', color: '#2d3436' },
  { name: 'Tesla Model X', capacity: 100, type: 'suv', color: '#f5f5f5' },
  { name: 'Tesla Cybertruck', capacity: 123, type: 'truck', color: '#8c8c8c' },
  { name: 'BMW i4', capacity: 84, type: 'sedan', color: '#0066b2' },
  { name: 'BMW iX', capacity: 112, type: 'suv', color: '#1c1c1c' },
  { name: 'Porsche Taycan', capacity: 93, type: 'sports', color: '#ba0c2f' },
  { name: 'VW ID.4', capacity: 77, type: 'suv', color: '#00a0d6' },
  { name: 'VW ID.3', capacity: 58, type: 'hatchback', color: '#ff6600' },
  { name: 'Hyundai Ioniq 5', capacity: 77, type: 'suv', color: '#4a6741' },
  { name: 'Hyundai Ioniq 6', capacity: 77, type: 'sedan', color: '#7b8e9e' },
  { name: 'Kia EV6', capacity: 77, type: 'suv', color: '#05141f' },
  { name: 'Ford Mustang Mach-E', capacity: 91, type: 'suv', color: '#d73b3e' },
  { name: 'Rivian R1T', capacity: 135, type: 'truck', color: '#1a472a' },
  { name: 'Polestar 2', capacity: 78, type: 'sedan', color: '#c4a35a' },
  { name: 'Mercedes EQS', capacity: 108, type: 'sedan', color: '#1a1a1a' },
  { name: 'Audi e-tron GT', capacity: 93, type: 'sports', color: '#bb0a30' },
  { name: 'Custom', capacity: 75, type: 'sedan', color: '#3498db' },
];

const themes = {
  tesla: {
    name: 'Tesla Dark',
    bg: 'bg-gradient-to-b from-gray-900 via-gray-900 to-black',
    card: 'bg-gray-800/50 border-gray-700/50',
    cardHover: 'hover:border-blue-500/50',
    accent: 'text-blue-400',
    accentBg: 'bg-blue-500',
    secondary: 'text-green-400',
    secondaryBg: 'bg-green-500',
    text: 'text-white',
    textMuted: 'text-gray-400',
    slider: 'accent-blue-500',
    summary: 'bg-gradient-to-br from-blue-900/50 to-blue-800/30 border-blue-500/30',
  },
  nordic: {
    name: 'Nordic Light',
    bg: 'bg-gradient-to-b from-slate-100 via-blue-50 to-white',
    card: 'bg-white/80 border-slate-200 shadow-sm',
    cardHover: 'hover:border-sky-400/50',
    accent: 'text-sky-600',
    accentBg: 'bg-sky-500',
    secondary: 'text-emerald-600',
    secondaryBg: 'bg-emerald-500',
    text: 'text-slate-800',
    textMuted: 'text-slate-500',
    slider: 'accent-sky-500',
    summary: 'bg-gradient-to-br from-sky-100 to-emerald-50 border-sky-300',
  },
  cyberpunk: {
    name: 'Cyberpunk',
    bg: 'bg-gradient-to-b from-purple-950 via-black to-fuchsia-950',
    card: 'bg-black/60 border-fuchsia-500/30',
    cardHover: 'hover:border-cyan-400/50',
    accent: 'text-cyan-400',
    accentBg: 'bg-cyan-500',
    secondary: 'text-fuchsia-400',
    secondaryBg: 'bg-fuchsia-500',
    text: 'text-white',
    textMuted: 'text-purple-300',
    slider: 'accent-cyan-400',
    summary: 'bg-gradient-to-br from-fuchsia-900/50 to-cyan-900/30 border-fuchsia-500/30',
  },
  eco: {
    name: 'Eco Green',
    bg: 'bg-gradient-to-b from-green-900 via-emerald-950 to-black',
    card: 'bg-emerald-900/40 border-emerald-700/50',
    cardHover: 'hover:border-lime-400/50',
    accent: 'text-lime-400',
    accentBg: 'bg-lime-500',
    secondary: 'text-emerald-300',
    secondaryBg: 'bg-emerald-500',
    text: 'text-white',
    textMuted: 'text-emerald-400',
    slider: 'accent-lime-400',
    summary: 'bg-gradient-to-br from-emerald-800/50 to-lime-900/30 border-lime-500/30',
  },
  sunset: {
    name: 'Sunset',
    bg: 'bg-gradient-to-b from-orange-900 via-rose-950 to-slate-950',
    card: 'bg-slate-900/60 border-orange-500/30',
    cardHover: 'hover:border-amber-400/50',
    accent: 'text-amber-400',
    accentBg: 'bg-amber-500',
    secondary: 'text-rose-400',
    secondaryBg: 'bg-rose-500',
    text: 'text-white',
    textMuted: 'text-orange-300',
    slider: 'accent-amber-400',
    summary: 'bg-gradient-to-br from-orange-900/50 to-rose-900/30 border-orange-500/30',
  },
  midnight: {
    name: 'Midnight Blue',
    bg: 'bg-gradient-to-b from-indigo-950 via-slate-950 to-black',
    card: 'bg-indigo-950/50 border-indigo-500/30',
    cardHover: 'hover:border-violet-400/50',
    accent: 'text-violet-400',
    accentBg: 'bg-violet-500',
    secondary: 'text-indigo-300',
    secondaryBg: 'bg-indigo-500',
    text: 'text-white',
    textMuted: 'text-indigo-400',
    slider: 'accent-violet-400',
    summary: 'bg-gradient-to-br from-indigo-900/50 to-violet-900/30 border-violet-500/30',
  },
  minimal: {
    name: 'Minimal',
    bg: 'bg-white',
    card: 'bg-gray-50 border-gray-200',
    cardHover: 'hover:border-gray-400',
    accent: 'text-gray-900',
    accentBg: 'bg-gray-900',
    secondary: 'text-gray-600',
    secondaryBg: 'bg-gray-600',
    text: 'text-gray-900',
    textMuted: 'text-gray-500',
    slider: 'accent-gray-900',
    summary: 'bg-gray-100 border-gray-300',
  },
  ocean: {
    name: 'Ocean',
    bg: 'bg-gradient-to-b from-cyan-900 via-blue-950 to-slate-950',
    card: 'bg-blue-950/50 border-cyan-500/30',
    cardHover: 'hover:border-teal-400/50',
    accent: 'text-teal-400',
    accentBg: 'bg-teal-500',
    secondary: 'text-cyan-300',
    secondaryBg: 'bg-cyan-500',
    text: 'text-white',
    textMuted: 'text-cyan-400',
    slider: 'accent-teal-400',
    summary: 'bg-gradient-to-br from-cyan-900/50 to-teal-900/30 border-teal-500/30',
  },
};

export default function ChargingTool() {
  const [theme, setTheme] = useState('tesla');
  const [selectedModel, setSelectedModel] = useState(evModels[0]);
  const [customCapacity, setCustomCapacity] = useState(75);
  const [currentSOC, setCurrentSOC] = useState(20);
  const [targetSOC, setTargetSOC] = useState(80);
  const [chargingSpeed, setChargingSpeed] = useState(16);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [phases, setPhases] = useState(3);
  const [voltage, setVoltage] = useState(230);
  const [startTime, setStartTime] = useState('11:00');
  const [showModelPicker, setShowModelPicker] = useState(false);
  const [showThemePicker, setShowThemePicker] = useState(false);
  
  const t = themes[theme];
  const capacity = selectedModel.name === 'Custom' ? customCapacity : selectedModel.capacity;
  const chargingPowerKW = (chargingSpeed * voltage * phases) / 1000;
  const chargingLoss = chargingPowerKW > 11 ? 0.08 : chargingPowerKW > 7 ? 0.10 : 0.12;
  const effectivePowerKW = chargingPowerKW * (1 - chargingLoss);
  const energyNeeded = capacity * (targetSOC - currentSOC) / 100;
  const chargingTimeHours = energyNeeded / effectivePowerKW;
  const hours = Math.floor(chargingTimeHours);
  const minutes = Math.floor((chargingTimeHours - hours) * 60);
  const consumedEnergy = energyNeeded / (1 - chargingLoss);
  const lostEnergy = consumedEnergy - energyNeeded;
  const spotPrice = 19.08;
  const totalCost = (consumedEnergy * spotPrice / 100).toFixed(2);
  
  const getEndTime = () => {
    const [h, m] = startTime.split(':').map(Number);
    const startMinutes = h * 60 + m;
    const endMinutes = startMinutes + chargingTimeHours * 60;
    const endH = Math.floor(endMinutes / 60) % 24;
    const endM = Math.floor(endMinutes % 60);
    return `${endH.toString().padStart(2, '0')}:${endM.toString().padStart(2, '0')}`;
  };

  const rangeAdded = Math.round(energyNeeded * 5.5);
  const CarSVG = carSVGs[selectedModel.type];

  return (
    <div className={`min-h-screen ${t.bg} ${t.text} p-4 max-w-md mx-auto transition-all duration-500`}>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-xl font-semibold">Charging</h1>
        <button onClick={() => setShowThemePicker(!showThemePicker)} className={`p-2 rounded-full ${t.card} border`}>
          <Palette className={`w-5 h-5 ${t.accent}`} />
        </button>
      </div>

      {/* Theme Picker */}
      {showThemePicker && (
        <div className={`${t.card} border rounded-2xl p-4 mb-4`}>
          <p className={`text-sm ${t.textMuted} mb-3`}>Choose Theme</p>
          <div className="grid grid-cols-4 gap-2">
            {Object.entries(themes).map(([key, val]) => (
              <button
                key={key}
                onClick={() => { setTheme(key); setShowThemePicker(false); }}
                className={`p-2 rounded-lg text-xs font-medium transition-all ${theme === key ? `${t.accentBg} text-white` : `${t.card} border ${t.textMuted}`}`}
              >
                {val.name.split(' ')[0]}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Car Visual & Model Selector */}
      <div className="relative mb-4">
        <div 
          onClick={() => setShowModelPicker(!showModelPicker)}
          className={`${t.card} border rounded-2xl p-4 cursor-pointer ${t.cardHover} transition-all`}
        >
          {/* Car Image */}
          <div className="h-20 mb-4 flex items-center justify-center">
            {CarSVG(selectedModel.color)}
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium">{selectedModel.name}</p>
              <p className={`text-sm ${t.textMuted}`}>{capacity} kWh battery</p>
            </div>
            <ChevronDown className={`w-5 h-5 ${t.textMuted} transition-transform ${showModelPicker ? 'rotate-180' : ''}`} />
          </div>
          
          {/* Quick Stats */}
          <div className="grid grid-cols-3 gap-4 mt-4 pt-4 border-t border-current/10">
            <div className="text-center">
              <p className={`text-2xl font-light ${t.accent}`}>{currentSOC}%</p>
              <p className={`text-xs ${t.textMuted}`}>Current</p>
            </div>
            <div className="text-center">
              <p className={`text-2xl font-light ${t.secondary}`}>{targetSOC}%</p>
              <p className={`text-xs ${t.textMuted}`}>Target</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-light">~{rangeAdded}km</p>
              <p className={`text-xs ${t.textMuted}`}>Range</p>
            </div>
          </div>
        </div>

        {/* Model Picker */}
        {showModelPicker && (
          <div className={`absolute top-full left-0 right-0 mt-2 ${t.card} border rounded-xl overflow-hidden z-10 max-h-72 overflow-y-auto`}>
            {evModels.map((model, i) => (
              <div
                key={i}
                onClick={() => { setSelectedModel(model); setShowModelPicker(false); }}
                className={`flex items-center gap-3 p-3 cursor-pointer transition-colors ${selectedModel.name === model.name ? `${t.accentBg}/20` : ''} hover:bg-current/5`}
              >
                <div className="w-16 h-8">
                  {carSVGs[model.type](model.color)}
                </div>
                <div>
                  <p className="font-medium text-sm">{model.name}</p>
                  <p className={`text-xs ${t.textMuted}`}>{model.capacity} kWh</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Custom Capacity */}
      {selectedModel.name === 'Custom' && (
        <div className={`${t.card} border rounded-2xl p-4 mb-4`}>
          <div className="flex justify-between mb-2">
            <span className={t.textMuted}>Battery Capacity</span>
            <span className="font-medium">{customCapacity} kWh</span>
          </div>
          <input type="range" min="20" max="150" value={customCapacity} onChange={(e) => setCustomCapacity(Number(e.target.value))} className={`w-full h-2 rounded-lg appearance-none cursor-pointer ${t.slider}`} />
        </div>
      )}

      {/* SOC Slider */}
      <div className={`${t.card} border rounded-2xl p-4 mb-4`}>
        <div className="flex justify-between items-center mb-4">
          <span className={`${t.textMuted} flex items-center gap-2`}><Battery className="w-4 h-4" /> Charge Level</span>
        </div>
        
        <div className="relative h-10 mb-4">
          <div className="absolute inset-0 bg-current/10 rounded-full overflow-hidden">
            <div className={`absolute h-full ${t.accentBg} transition-all opacity-80`} style={{ left: `${currentSOC}%`, width: `${targetSOC - currentSOC}%` }} />
            <div className={`absolute h-full ${t.accentBg}/30`} style={{ width: `${currentSOC}%` }} />
          </div>
          <input type="range" min="0" max="100" value={currentSOC} onChange={(e) => setCurrentSOC(Math.min(Number(e.target.value), targetSOC - 5))} className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
          <input type="range" min="0" max="100" value={targetSOC} onChange={(e) => setTargetSOC(Math.max(Number(e.target.value), currentSOC + 5))} className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
          <div className={`absolute top-1/2 -translate-y-1/2 ${t.accentBg} text-white text-xs font-bold px-2 py-1 rounded-full shadow-lg pointer-events-none`} style={{ left: `${currentSOC}%`, transform: 'translate(-50%, -50%)' }}>{currentSOC}%</div>
          <div className={`absolute top-1/2 -translate-y-1/2 ${t.secondaryBg} text-white text-xs font-bold px-2 py-1 rounded-full shadow-lg pointer-events-none`} style={{ left: `${targetSOC}%`, transform: 'translate(-50%, -50%)' }}>{targetSOC}%</div>
        </div>
        <div className={`flex justify-between text-sm ${t.textMuted}`}><span>0%</span><span>50%</span><span>100%</span></div>
      </div>

      {/* Charging Speed */}
      <div className={`${t.card} border rounded-2xl p-4 mb-4`}>
        <div className="flex justify-between items-center mb-4">
          <span className={`${t.textMuted} flex items-center gap-2`}><Zap className="w-4 h-4" /> Speed</span>
          <span className={`font-medium ${t.accent}`}>{chargingPowerKW.toFixed(1)} kW</span>
        </div>
        <input type="range" min="6" max="32" value={chargingSpeed} onChange={(e) => setChargingSpeed(Number(e.target.value))} className={`w-full h-2 rounded-lg appearance-none cursor-pointer ${t.slider} mb-2`} />
        <div className={`flex justify-between text-sm ${t.textMuted}`}><span>6A</span><span>{chargingSpeed}A</span><span>32A</span></div>

        <button onClick={() => setShowAdvanced(!showAdvanced)} className={`flex items-center gap-2 text-sm ${t.accent} mt-4`}>
          <Settings className="w-4 h-4" /> Advanced {showAdvanced ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </button>

        {showAdvanced && (
          <div className="mt-4 pt-4 border-t border-current/10 space-y-4">
            <div>
              <div className={`flex justify-between text-sm mb-2 ${t.textMuted}`}><span>Phases</span><span>{phases}</span></div>
              <div className="flex gap-2">
                {[1, 2, 3].map((p) => (
                  <button key={p} onClick={() => setPhases(p)} className={`flex-1 py-2 rounded-lg text-sm font-medium transition-all ${phases === p ? `${t.accentBg} text-white` : `${t.card} border ${t.textMuted}`}`}>{p}P</button>
                ))}
              </div>
            </div>
            <div>
              <div className={`flex justify-between text-sm mb-2 ${t.textMuted}`}><span>Voltage</span><span>{voltage}V</span></div>
              <input type="range" min="110" max="400" step="10" value={voltage} onChange={(e) => setVoltage(Number(e.target.value))} className={`w-full h-2 rounded-lg appearance-none cursor-pointer ${t.slider}`} />
            </div>
            <div className={`flex justify-between text-sm ${t.textMuted}`}><span>Loss (auto)</span><span className="text-amber-400">{(chargingLoss * 100).toFixed(0)}%</span></div>
          </div>
        )}
      </div>

      {/* Time */}
      <div className={`${t.card} border rounded-2xl p-4 mb-4`}>
        <div className="flex justify-between items-center mb-4">
          <span className={`${t.textMuted} flex items-center gap-2`}><Clock className="w-4 h-4" /> Schedule</span>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className={`text-xs ${t.textMuted} mb-1`}>Start</p>
            <input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} className={`w-full ${t.card} border rounded-lg p-3 text-center text-lg font-light`} />
          </div>
          <div>
            <p className={`text-xs ${t.textMuted} mb-1`}>End</p>
            <div className={`${t.card} border rounded-lg p-3 text-center text-lg font-light ${t.secondary}`}>{getEndTime()}</div>
          </div>
        </div>
        <div className="mt-4 text-center">
          <p className="text-3xl font-light">{hours}h {minutes}m</p>
          <p className={`text-sm ${t.textMuted}`}>duration</p>
        </div>
      </div>

      {/* Summary */}
      <div className={`${t.summary} border rounded-2xl p-4`}>
        <h3 className="text-lg font-medium mb-4">Summary</h3>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between"><span className={t.textMuted}>To battery</span><span>{energyNeeded.toFixed(2)} kWh</span></div>
          <div className="flex justify-between"><span className={t.textMuted}>Consumed</span><span>{consumedEnergy.toFixed(2)} kWh</span></div>
          <div className="flex justify-between"><span className={t.textMuted}>Lost</span><span className="text-amber-400">{lostEnergy.toFixed(2)} kWh</span></div>
          <div className="flex justify-between"><span className={t.textMuted}>Spot price</span><span>{spotPrice} c/kWh</span></div>
          <div className="border-t border-current/10 pt-3 mt-3">
            <div className="flex justify-between items-center">
              <span className={t.textMuted}>Cost</span>
              <span className={`text-2xl font-semibold ${t.secondary}`}>{totalCost} €</span>
            </div>
          </div>
        </div>
      </div>

      <p className={`text-center text-xs ${t.textMuted} mt-6`}>aut.fi • Auto Liukuri</p>
    </div>
  );
}
