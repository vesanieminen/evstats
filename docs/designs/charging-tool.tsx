import { useState } from 'react';
import { ChevronDown, ChevronUp, Settings, Zap, Battery, Clock, Calendar, Gauge, Car, Euro } from 'lucide-react';

const evModels = [
  { name: 'Tesla Model 3 LR', capacity: 75, efficiency: 14.5 },
  { name: 'Tesla Model Y LR', capacity: 75, efficiency: 15.2 },
  { name: 'Tesla Model S', capacity: 100, efficiency: 16.1 },
  { name: 'Tesla Model X', capacity: 100, efficiency: 18.5 },
  { name: 'BMW iX xDrive50', capacity: 105, efficiency: 19.8 },
  { name: 'Mercedes EQS 450+', capacity: 108, efficiency: 17.5 },
  { name: 'Audi e-tron GT', capacity: 93, efficiency: 19.2 },
  { name: 'Porsche Taycan', capacity: 93, efficiency: 18.8 },
  { name: 'Volkswagen ID.4', capacity: 77, efficiency: 16.5 },
  { name: 'Hyundai Ioniq 6', capacity: 77, efficiency: 14.0 },
  { name: 'Polestar 2 LR', capacity: 78, efficiency: 16.8 },
  { name: 'Ford Mustang Mach-E', capacity: 91, efficiency: 17.2 },
  { name: 'Custom', capacity: 75, efficiency: 16.0 },
];

const themes = {
  tesla: {
    name: 'Tesla Dark',
    bg: 'bg-gradient-to-b from-gray-900 via-gray-800 to-black',
    card: 'bg-gray-800/50 backdrop-blur border border-gray-700/50',
    accent: 'from-blue-500 to-cyan-400',
    accentSolid: 'bg-blue-500',
    accentText: 'text-cyan-400',
    text: 'text-white',
    textMuted: 'text-gray-400',
    slider: 'bg-gray-700',
    sliderFill: 'bg-gradient-to-r from-blue-500 to-cyan-400',
    input: 'bg-gray-700/50 border-gray-600',
    glow: 'shadow-lg shadow-blue-500/20',
    divider: 'border-gray-700',
    btnActive: 'bg-gradient-to-r from-blue-500 to-cyan-400 text-white',
    btnInactive: 'bg-gray-700/50 text-gray-400',
  },
  midnight: {
    name: 'Midnight Purple',
    bg: 'bg-gradient-to-b from-purple-950 via-indigo-950 to-black',
    card: 'bg-purple-900/30 backdrop-blur border border-purple-700/30',
    accent: 'from-purple-500 to-pink-500',
    accentSolid: 'bg-purple-500',
    accentText: 'text-pink-400',
    text: 'text-white',
    textMuted: 'text-purple-300',
    slider: 'bg-purple-900',
    sliderFill: 'bg-gradient-to-r from-purple-500 to-pink-500',
    input: 'bg-purple-900/50 border-purple-700',
    glow: 'shadow-lg shadow-purple-500/20',
    divider: 'border-purple-700/50',
    btnActive: 'bg-gradient-to-r from-purple-500 to-pink-500 text-white',
    btnInactive: 'bg-purple-900/50 text-purple-300',
  },
  aurora: {
    name: 'Aurora Green',
    bg: 'bg-gradient-to-b from-emerald-950 via-teal-950 to-black',
    card: 'bg-emerald-900/30 backdrop-blur border border-emerald-700/30',
    accent: 'from-emerald-400 to-teal-400',
    accentSolid: 'bg-emerald-500',
    accentText: 'text-teal-400',
    text: 'text-white',
    textMuted: 'text-emerald-300',
    slider: 'bg-emerald-900',
    sliderFill: 'bg-gradient-to-r from-emerald-400 to-teal-400',
    input: 'bg-emerald-900/50 border-emerald-700',
    glow: 'shadow-lg shadow-emerald-500/20',
    divider: 'border-emerald-700/50',
    btnActive: 'bg-gradient-to-r from-emerald-400 to-teal-400 text-black',
    btnInactive: 'bg-emerald-900/50 text-emerald-300',
  },
  frost: {
    name: 'Frost Light',
    bg: 'bg-gradient-to-b from-slate-100 via-blue-50 to-white',
    card: 'bg-white/70 backdrop-blur border border-slate-200',
    accent: 'from-blue-600 to-indigo-600',
    accentSolid: 'bg-blue-600',
    accentText: 'text-blue-600',
    text: 'text-slate-900',
    textMuted: 'text-slate-500',
    slider: 'bg-slate-200',
    sliderFill: 'bg-gradient-to-r from-blue-600 to-indigo-600',
    input: 'bg-white border-slate-300',
    glow: 'shadow-lg shadow-blue-500/10',
    divider: 'border-slate-200',
    btnActive: 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white',
    btnInactive: 'bg-slate-100 text-slate-500',
  },
};

const DualRangeSlider = ({ min, max, lowVal, highVal, onLowChange, onHighChange, theme }) => {
  const t = themes[theme];
  const lowPct = ((lowVal - min) / (max - min)) * 100;
  const highPct = ((highVal - min) / (max - min)) * 100;
  
  const handleTrackClick = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const pct = ((e.clientX - rect.left) / rect.width) * 100;
    const val = Math.round((pct / 100) * (max - min) + min);
    const distToLow = Math.abs(val - lowVal);
    const distToHigh = Math.abs(val - highVal);
    if (distToLow < distToHigh) {
      onLowChange(Math.min(val, highVal - 5));
    } else {
      onHighChange(Math.max(val, lowVal + 5));
    }
  };

  return (
    <div className="relative h-16 flex items-center pt-2">
      <div className={`absolute w-full h-2 rounded-full ${t.slider} cursor-pointer`} onClick={handleTrackClick} />
      <div className={`absolute h-2 rounded-full ${t.sliderFill} pointer-events-none`} style={{ left: `${lowPct}%`, width: `${highPct - lowPct}%` }} />
      
      <input type="range" min={min} max={max} value={lowVal}
        onChange={(e) => onLowChange(Math.min(Number(e.target.value), highVal - 5))}
        className="absolute w-full h-2 appearance-none bg-transparent cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-6 [&::-webkit-slider-thumb]:h-6 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-lg [&::-webkit-slider-thumb]:cursor-grab [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-blue-500 [&::-webkit-slider-thumb]:relative [&::-webkit-slider-thumb]:z-20"
        style={{ zIndex: lowVal > max - 10 ? 30 : 20 }} />
      <input type="range" min={min} max={max} value={highVal}
        onChange={(e) => onHighChange(Math.max(Number(e.target.value), lowVal + 5))}
        className="absolute w-full h-2 appearance-none bg-transparent cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-6 [&::-webkit-slider-thumb]:h-6 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-lg [&::-webkit-slider-thumb]:cursor-grab [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-cyan-400 [&::-webkit-slider-thumb]:relative [&::-webkit-slider-thumb]:z-10" />
      
      <div className="absolute w-full flex justify-between top-12 text-sm font-medium">
        <span className={t.text}>{lowVal}%</span>
        <span className={t.text}>{highVal}%</span>
      </div>
    </div>
  );
};

const CarVisualization = ({ currentSoc, targetSoc, theme, model, currentRange, targetRange }) => {
  const t = themes[theme];
  const isLight = theme === 'frost';
  
  return (
    <div className="relative flex flex-col items-center py-4">
      <svg viewBox="0 0 200 80" className="w-48 h-20 mb-2">
        <defs>
          <linearGradient id={`carGrad-${theme}`} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor={isLight ? '#334155' : '#4B5563'} />
            <stop offset="100%" stopColor={isLight ? '#1e293b' : '#1F2937'} />
          </linearGradient>
          <linearGradient id={`windowGrad-${theme}`} x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor={isLight ? '#60a5fa' : '#3B82F6'} />
            <stop offset="100%" stopColor={isLight ? '#3b82f6' : '#1E40AF'} />
          </linearGradient>
        </defs>
        <ellipse cx="45" cy="65" rx="18" ry="18" fill={isLight ? '#1e293b' : '#111'} />
        <ellipse cx="45" cy="65" rx="12" ry="12" fill={isLight ? '#64748b' : '#374151'} />
        <ellipse cx="155" cy="65" rx="18" ry="18" fill={isLight ? '#1e293b' : '#111'} />
        <ellipse cx="155" cy="65" rx="12" ry="12" fill={isLight ? '#64748b' : '#374151'} />
        <path d="M20 50 Q25 25 60 20 L140 20 Q175 25 180 50 L180 55 Q180 60 175 60 L25 60 Q20 60 20 55 Z" fill={`url(#carGrad-${theme})`} />
        <path d="M55 22 Q60 12 80 10 L120 10 Q140 12 145 22 L140 20 L60 20 Z" fill={`url(#windowGrad-${theme})`} opacity="0.9" />
        <rect x="5" y="48" width="15" height="6" rx="2" fill="#fbbf24" opacity="0.8" />
        <rect x="180" y="48" width="15" height="6" rx="2" fill="#ef4444" opacity="0.8" />
      </svg>
      <p className={`text-sm ${t.textMuted}`}>{model.name}</p>
      <div className="grid grid-cols-3 gap-4 text-center w-full mt-4">
        <div>
          <p className={`text-xs ${t.textMuted}`}>Current</p>
          <p className="text-xl font-semibold">{currentSoc}%</p>
          <p className={`text-xs ${t.textMuted}`}>{currentRange} km</p>
        </div>
        <div>
          <p className={`text-xs ${t.textMuted}`}>Range +</p>
          <p className={`text-xl font-semibold ${t.accentText}`}>{targetRange - currentRange} km</p>
          <p className={`text-xs ${t.textMuted}`}>added</p>
        </div>
        <div>
          <p className={`text-xs ${t.textMuted}`}>Target</p>
          <p className="text-xl font-semibold">{targetSoc}%</p>
          <p className={`text-xs ${t.textMuted}`}>{targetRange} km</p>
        </div>
      </div>
    </div>
  );
};

export default function ChargingTool() {
  const [theme, setTheme] = useState('tesla');
  const [showThemes, setShowThemes] = useState(false);
  const [selectedModel, setSelectedModel] = useState(evModels[0]);
  const [showVehicleSelect, setShowVehicleSelect] = useState(false);
  const [customCapacity, setCustomCapacity] = useState(75);
  const [customEfficiency, setCustomEfficiency] = useState(16);
  const [currentSoc, setCurrentSoc] = useState(20);
  const [targetSoc, setTargetSoc] = useState(80);
  const [chargingAmps, setChargingAmps] = useState(16);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [phases, setPhases] = useState(3);
  const [voltage, setVoltage] = useState(230);
  const [chargingLossAuto, setChargingLossAuto] = useState(true);
  const [chargingLossManual, setChargingLossManual] = useState(10);
  const [startDate, setStartDate] = useState('2026-01-26');
  const [startTime, setStartTime] = useState('11:00');
  const [endDate, setEndDate] = useState('2026-01-26');
  const [endTime, setEndTime] = useState('13:15');
  const [calcMode, setCalcMode] = useState('end');
  const [spotPrice, setSpotPrice] = useState(19.08);

  const t = themes[theme];
  const capacity = selectedModel.name === 'Custom' ? customCapacity : selectedModel.capacity;
  const efficiency = selectedModel.name === 'Custom' ? customEfficiency : selectedModel.efficiency;
  const currentRange = Math.round((capacity * currentSoc / 100) / efficiency * 100);
  const targetRange = Math.round((capacity * targetSoc / 100) / efficiency * 100);
  const chargingPowerKw = (chargingAmps * voltage * phases) / 1000;
  const autoChargingLoss = chargingPowerKw > 11 ? 8 : chargingPowerKw > 7 ? 10 : 12;
  const chargingLoss = chargingLossAuto ? autoChargingLoss : chargingLossManual;
  const effectivePower = chargingPowerKw * (1 - chargingLoss / 100);
  const energyNeeded = capacity * (targetSoc - currentSoc) / 100;
  const energyConsumed = energyNeeded / (1 - chargingLoss / 100);
  const chargingTimeHours = energyNeeded / effectivePower;
  const hours = Math.floor(chargingTimeHours);
  const minutes = Math.floor((chargingTimeHours - hours) * 60);
  const totalCost = (energyConsumed * spotPrice / 100).toFixed(2);
  const lostEnergy = (energyConsumed - energyNeeded).toFixed(2);

  return (
    <div className={`min-h-screen ${t.bg} ${t.text} p-4`}>
      <div className="max-w-md mx-auto space-y-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-light tracking-wide">Auto Liukuri</h1>
          <button onClick={() => setShowThemes(!showThemes)} className={`p-2 rounded-full ${t.card}`}>
            <Settings size={20} />
          </button>
        </div>

        {showThemes && (
          <div className={`${t.card} rounded-2xl p-4 ${t.glow}`}>
            <p className={`text-sm ${t.textMuted} mb-3`}>Choose Theme</p>
            <div className="grid grid-cols-2 gap-2">
              {Object.entries(themes).map(([key, val]) => (
                <button key={key} onClick={() => { setTheme(key); setShowThemes(false); }}
                  className={`p-3 rounded-xl text-sm font-medium transition-all ${theme === key ? `bg-gradient-to-r ${val.accent} text-white` : t.input} border`}>
                  {val.name}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Car Section with Vehicle Selection */}
        <div className={`${t.card} rounded-2xl p-4 ${t.glow}`}>
          <CarVisualization currentSoc={currentSoc} targetSoc={targetSoc} theme={theme} model={selectedModel} currentRange={currentRange} targetRange={targetRange} />
          <button onClick={() => setShowVehicleSelect(!showVehicleSelect)}
            className={`w-full mt-2 flex items-center justify-center gap-2 text-sm ${t.textMuted} py-2`}>
            <Car size={16} /> Change Vehicle {showVehicleSelect ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </button>
          {showVehicleSelect && (
            <div className={`mt-3 pt-3 border-t ${t.divider}`}>
              <select value={selectedModel.name} onChange={(e) => { setSelectedModel(evModels.find(m => m.name === e.target.value)); setShowVehicleSelect(false); }}
                className={`w-full p-3 rounded-xl ${t.input} ${t.text} border outline-none`}>
                {evModels.map(m => <option key={m.name} value={m.name}>{m.name} ({m.capacity} kWh)</option>)}
              </select>
              {selectedModel.name === 'Custom' && (
                <div className="grid grid-cols-2 gap-3 mt-3">
                  <div>
                    <label className={`text-xs ${t.textMuted}`}>Battery Capacity (kWh)</label>
                    <input type="number" value={customCapacity} onChange={(e) => setCustomCapacity(Number(e.target.value))}
                      className={`w-full mt-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm`} />
                  </div>
                  <div>
                    <label className={`text-xs ${t.textMuted}`}>Consumption (kWh/100km)</label>
                    <input type="number" step="0.1" value={customEfficiency} onChange={(e) => setCustomEfficiency(Number(e.target.value))}
                      className={`w-full mt-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm`} />
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* SOC Slider */}
        <div className={`${t.card} rounded-2xl p-4 ${t.glow}`}>
          <div className="flex items-center gap-2 mb-1">
            <Battery size={18} className={t.accentText} />
            <label className={`text-sm ${t.textMuted}`}>Charge Level</label>
          </div>
          <DualRangeSlider min={0} max={100} lowVal={currentSoc} highVal={targetSoc} onLowChange={setCurrentSoc} onHighChange={setTargetSoc} theme={theme} />
          <div className="flex justify-between mt-6 text-sm">
            <span>Adding: <strong>{energyNeeded.toFixed(1)} kWh</strong></span>
            <span className={t.textMuted}>{capacity} kWh battery</span>
          </div>
        </div>

        {/* Charging Speed */}
        <div className={`${t.card} rounded-2xl p-4 ${t.glow}`}>
          <div className="flex justify-between items-center mb-3">
            <div className="flex items-center gap-2">
              <Zap size={18} className={t.accentText} />
              <label className={`text-sm ${t.textMuted}`}>Charging Speed</label>
            </div>
            <span className="text-lg font-semibold">{chargingPowerKw.toFixed(1)} kW</span>
          </div>
          <input type="range" min={1} max={32} value={chargingAmps} onChange={(e) => setChargingAmps(Number(e.target.value))}
            className={`w-full h-2 rounded-full appearance-none ${t.slider} [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-lg [&::-webkit-slider-thumb]:cursor-grab`} />
          <div className="flex justify-center mt-2">
            <span className={`text-sm ${t.textMuted}`}>{chargingAmps} A</span>
          </div>
          <button onClick={() => setShowAdvanced(!showAdvanced)}
            className={`w-full mt-3 flex items-center justify-center gap-2 text-sm ${t.textMuted} py-2`}>
            Advanced {showAdvanced ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </button>
          {showAdvanced && (
            <div className={`grid grid-cols-2 gap-3 mt-3 pt-3 border-t ${t.divider}`}>
              <div>
                <label className={`text-xs ${t.textMuted}`}>Phases</label>
                <select value={phases} onChange={(e) => setPhases(Number(e.target.value))}
                  className={`w-full mt-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm`}>
                  <option value={1}>1 Phase</option><option value={3}>3 Phase</option>
                </select>
              </div>
              <div>
                <label className={`text-xs ${t.textMuted}`}>Voltage</label>
                <select value={voltage} onChange={(e) => setVoltage(Number(e.target.value))}
                  className={`w-full mt-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm`}>
                  <option value={220}>220V</option><option value={230}>230V</option><option value={240}>240V</option><option value={400}>400V</option>
                </select>
              </div>
              <div className="col-span-2">
                <label className={`text-xs ${t.textMuted}`}>Charging Loss (%)</label>
                <div className="flex items-center gap-2 mt-1">
                  <button onClick={() => setChargingLossAuto(true)}
                    className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${chargingLossAuto ? t.btnActive : t.btnInactive}`}>
                    Auto
                  </button>
                  <input type="number" min={0} max={30} value={chargingLossAuto ? autoChargingLoss : chargingLossManual}
                    onChange={(e) => { setChargingLossAuto(false); setChargingLossManual(Number(e.target.value)); }}
                    className={`flex-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm ${chargingLossAuto ? 'opacity-50' : ''}`} />
                  <span className={`text-sm ${t.textMuted}`}>%</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Schedule */}
        <div className={`${t.card} rounded-2xl p-4 ${t.glow}`}>
          <div className="flex items-center gap-2 mb-4">
            <Calendar size={18} className={t.accentText} />
            <label className={`text-sm ${t.textMuted}`}>Schedule</label>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={`text-xs ${t.textMuted}`}>Start Date</label>
              <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)}
                className={`w-full mt-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm`} />
            </div>
            <div>
              <label className={`text-xs ${t.textMuted}`}>End Date</label>
              <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)}
                className={`w-full mt-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm`} />
            </div>
            <div>
              <label className={`text-xs ${t.textMuted}`}>Start Time</label>
              <input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)}
                readOnly={calcMode === 'start'}
                className={`w-full mt-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm ${calcMode === 'start' ? 'opacity-50 cursor-not-allowed' : ''}`} />
            </div>
            <div>
              <label className={`text-xs ${t.textMuted}`}>End Time</label>
              <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)}
                readOnly={calcMode === 'end'}
                className={`w-full mt-1 p-2 rounded-lg ${t.input} ${t.text} border outline-none text-sm ${calcMode === 'end' ? 'opacity-50 cursor-not-allowed' : ''}`} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2 mt-4">
            <button onClick={() => setCalcMode('end')}
              className={`p-2 rounded-xl text-sm font-medium transition-all ${calcMode === 'end' ? t.btnActive : t.btnInactive}`}>
              Calculate End
            </button>
            <button onClick={() => setCalcMode('start')}
              className={`p-2 rounded-xl text-sm font-medium transition-all ${calcMode === 'start' ? t.btnActive : t.btnInactive}`}>
              Calculate Start
            </button>
          </div>
        </div>

        {/* Results */}
        <div className={`${t.card} rounded-2xl p-4 ${t.glow}`}>
          <div className="flex items-center gap-2 mb-4">
            <Euro size={18} className={t.accentText} />
            <h3 className="font-semibold">Charging Summary</h3>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between"><span className={t.textMuted}><Clock size={14} className="inline mr-1" />Duration</span><span className="font-medium">{hours}h {minutes}min</span></div>
            <div className="flex justify-between"><span className={t.textMuted}>Energy consumed</span><span>{energyConsumed.toFixed(2)} kWh</span></div>
            <div className="flex justify-between"><span className={t.textMuted}>Added to battery</span><span>{energyNeeded.toFixed(2)} kWh</span></div>
            <div className="flex justify-between"><span className={t.textMuted}>Lost to heat</span><span className="text-orange-400">{lostEnergy} kWh</span></div>
            <div className="flex justify-between"><span className={t.textMuted}>Spot price</span><span>{spotPrice} c/kWh</span></div>
            <div className={`flex justify-between pt-3 border-t ${t.divider}`}>
              <span className="font-semibold">Total Cost</span>
              <span className={`text-xl font-bold bg-gradient-to-r ${t.accent} bg-clip-text text-transparent`}>{totalCost} €</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
