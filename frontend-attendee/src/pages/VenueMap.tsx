import React, { useState, useEffect, useMemo } from 'react';
import { 
  MapPin as MapPinIcon, 
  Search, 
  RefreshCw, 
  Utensils, 
  Wine, 
  Music, 
  Activity, 
  Users, 
  Clock, 
  Compass, 
  Sparkles,
  Info,
  ZoomIn,
  ZoomOut,
  CheckCircle2,
  LogIn,
  CreditCard,
  X
} from 'lucide-react';
import { useI18n } from '../context/I18nContext';

type DensityLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';

interface MapPinData {
  id: number;
  type: string;
  name: string;
  description: string;
  xpos: number;
  ypos: number;
}

interface MapLayoutData {
  id: number;
  imageUrl: string;
  active: boolean;
  pins: MapPinData[];
}

interface ZoneDensity {
  mapPinId: number;
  mapPinName: string;
  mapPinType: string;
  currentLevel: DensityLevel;
  averageScore: number;
  reportCount: number;
  lastReportedAt: string;
  lastReportedBy: string;
}

const DENSITY_META: Record<DensityLevel, {
  label: { en: string; ro: string };
  color: string;
  bgColor: string;
  borderColor: string;
  badgeBg: string;
  queueTime: { en: string; ro: string };
  stageStatus: { en: string; ro: string };
}> = {
  LOW: {
    label: { en: 'Low Crowd', ro: 'Aglomerație Scăzută' },
    color: '#10B981',
    bgColor: 'rgba(16, 185, 129, 0.18)',
    borderColor: 'rgba(16, 185, 129, 0.6)',
    badgeBg: 'rgba(16, 185, 129, 0.25)',
    queueTime: { en: '0 - 5 min wait', ro: '0 - 5 min așteptare' },
    stageStatus: { en: 'Clear / Easy Access', ro: 'Acces Liber' },
  },
  MODERATE: {
    label: { en: 'Moderate', ro: 'Aglomerație Moderată' },
    color: '#F59E0B',
    bgColor: 'rgba(245, 158, 11, 0.18)',
    borderColor: 'rgba(245, 158, 11, 0.6)',
    badgeBg: 'rgba(245, 158, 11, 0.25)',
    queueTime: { en: '5 - 15 min wait', ro: '5 - 15 min așteptare' },
    stageStatus: { en: 'Moderate Crowd', ro: 'Aglomerație Moderată' },
  },
  HIGH: {
    label: { en: 'High Surge', ro: 'Aglomerație Mare' },
    color: '#F97316',
    bgColor: 'rgba(249, 115, 22, 0.2)',
    borderColor: 'rgba(249, 115, 22, 0.7)',
    badgeBg: 'rgba(249, 115, 22, 0.3)',
    queueTime: { en: '15 - 30 min wait', ro: '15 - 30 min așteptare' },
    stageStatus: { en: 'Dense Crowd', ro: 'Mulțime Desă' },
  },
  CRITICAL: {
    label: { en: 'Critical Surge', ro: 'Aglomerație Critică' },
    color: '#EF4444',
    bgColor: 'rgba(239, 68, 68, 0.25)',
    borderColor: 'rgba(239, 68, 68, 0.9)',
    badgeBg: 'rgba(239, 68, 68, 0.35)',
    queueTime: { en: '30+ min wait', ro: '30+ min așteptare' },
    stageStatus: { en: 'Near Capacity', ro: 'Capacitate Maximă' },
  },
};

const CATEGORIES = [
  { id: 'ALL', label: { en: 'All Zones', ro: 'Toate Zonele' }, icon: Compass },
  { id: 'STAGES', label: { en: 'Stages & Shows', ro: 'Scene & Spectacole' }, types: ['STAGE', 'FANTASIA', 'WELLNESS'], icon: Music },
  { id: 'FOOD_BAR', label: { en: 'Food & Bars', ro: 'Mâncare & Băuturi' }, types: ['FOOD_COURT', 'BAR', 'CREDIT_POINT', 'VENDOR_ACTIVATION'], icon: Utensils },
  { id: 'UTILITIES', label: { en: 'Utilities & Gates', ro: 'Utilități & Porți' }, types: ['ENTRANCE', 'EXIT', 'TOILET', 'FIRST_AID'], icon: Activity },
];

export const VenueMap: React.FC = () => {
  const { language } = useI18n();
  const lang = language === 'ro' ? 'ro' : 'en';

  const [mapLayout, setMapLayout] = useState<MapLayoutData | null>(null);
  const [densityList, setDensityList] = useState<ZoneDensity[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedPin, setSelectedPin] = useState<MapPinData | null>(null);
  const [showWaitTimeBadge, setShowWaitTimeBadge] = useState<boolean>(true);
  const [zoomLevel, setZoomLevel] = useState<number>(1);
  const [lastRefreshed, setLastRefreshed] = useState<Date>(new Date());
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  useEffect(() => {
    fetchData();
    const interval = setInterval(() => {
      fetchDensityOnly();
    }, 30000); // auto-refresh every 30s
    return () => clearInterval(interval);
  }, []);

  const fetchData = async () => {
    setLoading(true);
    await Promise.all([fetchMap(), fetchDensityOnly()]);
    setLoading(false);
  };

  const fetchMap = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/map/active');
      const data = await res.json();
      if (data && data.type === 'SUCCESS' && data.data) {
        setMapLayout(data.data);
      }
    } catch (err) {
      console.error('Failed to load active venue map', err);
    }
  };

  const fetchDensityOnly = async () => {
    setIsRefreshing(true);
    try {
      const res = await fetch('http://localhost:8080/api/crowd-reports/aggregation?windowMinutes=60');
      const data = await res.json();
      if (data && data.type === 'SUCCESS' && data.data) {
        setDensityList(data.data);
        setLastRefreshed(new Date());
      }
    } catch (err) {
      console.error('Failed to load crowd density aggregation', err);
    } finally {
      setIsRefreshing(false);
    }
  };

  // Map pins merged with live density level
  const densityMap = useMemo(() => {
    const map = new Map<number, ZoneDensity>();
    densityList.forEach((d) => map.set(d.mapPinId, d));
    return map;
  }, [densityList]);

  // Sample pins fallback if backend has no active pins saved yet
  const pins: MapPinData[] = useMemo(() => {
    if (mapLayout && mapLayout.pins && mapLayout.pins.length > 0) {
      return mapLayout.pins;
    }
    return [
      { id: 101, name: 'Mainstage Arena', type: 'STAGE', description: 'Central arena for headliners', xpos: 50, ypos: 35 },
      { id: 102, name: 'Alchemy Stage', type: 'STAGE', description: 'Bass and hip-hop stage', xpos: 22, ypos: 60 },
      { id: 103, name: 'Daydreaming Stage', type: 'STAGE', description: 'Deep house and ambient vibe', xpos: 78, ypos: 55 },
      { id: 104, name: 'Food Court North', type: 'FOOD_COURT', description: 'Gourmet trucks and dining area', xpos: 38, ypos: 72 },
      { id: 105, name: 'Cocktail Bar East', type: 'BAR', description: 'Craft cocktails and cold beer', xpos: 65, ypos: 42 },
      { id: 106, name: 'Main Entrance Gate A', type: 'ENTRANCE', description: 'Fast track and VIP wristband scan', xpos: 15, ypos: 20 },
      { id: 107, name: 'Central Restrooms', type: 'TOILET', description: 'Sanitation & water refill station', xpos: 48, ypos: 80 },
      { id: 108, name: 'Cashless Credit Point', type: 'CREDIT_POINT', description: 'Wristband top-up & cash refund', xpos: 82, ypos: 25 },
      { id: 109, name: 'First Aid Tent', type: 'FIRST_AID', description: '24/7 Medical assistance', xpos: 30, ypos: 40 },
    ];
  }, [mapLayout]);

  const filteredPins = useMemo(() => {
    return pins.filter((pin) => {
      // Category filter
      if (activeCategory !== 'ALL') {
        const catObj = CATEGORIES.find((c) => c.id === activeCategory);
        if (catObj && catObj.types && !catObj.types.includes(pin.type)) {
          return false;
        }
      }
      // Search filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesName = pin.name.toLowerCase().includes(q);
        const matchesType = pin.type.toLowerCase().includes(q);
        const matchesDesc = (pin.description || '').toLowerCase().includes(q);
        return matchesName || matchesType || matchesDesc;
      }
      return true;
    });
  }, [pins, activeCategory, searchQuery]);

  const getPinIcon = (type: string) => {
    switch (type) {
      case 'STAGE': return Music;
      case 'BAR': return Wine;
      case 'FOOD_COURT': return Utensils;
      case 'CREDIT_POINT': return CreditCard;
      case 'ENTRANCE': return LogIn;
      case 'TOILET': return Users;
      case 'FIRST_AID': return Activity;
      default: return MapPinIcon;
    }
  };

  const getPinDensity = (pinId: number): ZoneDensity | null => {
    return densityMap.get(pinId) || null;
  };

  const getPinLevel = (pinId: number): DensityLevel => {
    const density = getPinDensity(pinId);
    return density ? density.currentLevel : 'LOW';
  };

  const getEstimatedWait = (pin: MapPinData) => {
    const level = getPinLevel(pin.id);
    const meta = DENSITY_META[level];
    const isService = ['FOOD_COURT', 'BAR', 'CREDIT_POINT', 'TOILET', 'ENTRANCE'].includes(pin.type);
    return isService ? meta.queueTime[lang] : meta.stageStatus[lang];
  };

  // Metrics for overview bar
  const criticalSurgeCount = useMemo(() => {
    return pins.filter((p) => getPinLevel(p.id) === 'CRITICAL' || getPinLevel(p.id) === 'HIGH').length;
  }, [pins, densityMap]);

  return (
    <main className="min-h-screen bg-background text-white pt-24 pb-16 px-margin-mobile md:px-margin-desktop relative overflow-hidden">
      {/* Background ambient lighting */}
      <div className="absolute top-10 left-1/4 w-96 h-96 bg-primary/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-10 right-1/4 w-[500px] h-[500px] bg-tertiary/10 rounded-full blur-3xl pointer-events-none" />

      {/* Header section */}
      <div className="max-w-7xl mx-auto mb-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/15 border border-primary/30 text-primary font-label-sm text-label-sm uppercase tracking-widest mb-3">
              <Sparkles className="w-3.5 h-3.5" />
              {lang === 'ro' ? 'Hartă Interactivă & Fluxuri' : 'Interactive Map & Crowd Heatmap'}
            </div>
            <h1 className="text-3xl md:text-5xl font-headline-lg uppercase tracking-tight text-white font-extrabold drop-shadow-[0_0_15px_rgba(255,255,255,0.4)]">
              {lang === 'ro' ? 'Estimator Timp Așteptare & Aglomerație' : 'Live Venue Crowd Heatmap'}
            </h1>
            <p className="text-on-surface-variant text-sm md:text-base max-w-2xl mt-1">
              {lang === 'ro'
                ? 'Verifică în timp real gradul de aglomerație și timpii estimați de așteptare la baruri, restaurante, porți și scene.'
                : 'Monitor real-time crowd density and estimated wait times at food courts, bars, entry gates, and stage perimeters.'}
            </p>
          </div>

          {/* Real-time status badge */}
          <div className="flex items-center gap-3">
            <button
              onClick={fetchDensityOnly}
              disabled={isRefreshing}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-surface-container-high border border-white/10 hover:border-primary/50 text-white font-label-sm text-xs transition-all hover:bg-white/10 active:scale-95 shadow-lg"
            >
              <RefreshCw className={`w-4 h-4 text-primary ${isRefreshing ? 'animate-spin' : ''}`} />
              <span>{isRefreshing ? (lang === 'ro' ? 'Actualizare...' : 'Refreshing...') : (lang === 'ro' ? 'Actualizează' : 'Sync Crowd Data')}</span>
            </button>

            <div className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-surface-container-high/80 border border-white/10 text-xs">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
              <span className="text-white/60 font-mono">
                {lang === 'ro' ? 'Sync ' : 'Updated '} {lastRefreshed.toLocaleTimeString()}
              </span>
            </div>
          </div>
        </div>

        {/* Quick Metrics Bar */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
          <div className="p-4 rounded-xl bg-surface-container-high/60 border border-white/10 backdrop-blur-md">
            <p className="text-xs text-white/50 uppercase font-semibold">{lang === 'ro' ? 'Puncte de Interes' : 'Total Venue Pins'}</p>
            <p className="text-2xl font-bold text-white mt-1">{pins.length}</p>
          </div>
          <div className="p-4 rounded-xl bg-surface-container-high/60 border border-white/10 backdrop-blur-md">
            <p className="text-xs text-white/50 uppercase font-semibold">{lang === 'ro' ? 'Medie Așteptare Bar' : 'Avg Bar Wait Time'}</p>
            <p className="text-2xl font-bold text-amber-400 mt-1">~5-10 min</p>
          </div>
          <div className="p-4 rounded-xl bg-surface-container-high/60 border border-white/10 backdrop-blur-md">
            <p className="text-xs text-white/50 uppercase font-semibold">{lang === 'ro' ? 'Aglomerație Mare' : 'High Surge Zones'}</p>
            <p className="text-2xl font-bold text-orange-400 mt-1">{criticalSurgeCount} {lang === 'ro' ? 'Zone' : 'Zones'}</p>
          </div>
          <div className="p-4 rounded-xl bg-surface-container-high/60 border border-white/10 backdrop-blur-md">
            <p className="text-xs text-white/50 uppercase font-semibold">{lang === 'ro' ? 'Stare Porți Acces' : 'Entrance Gate Status'}</p>
            <p className="text-2xl font-bold text-emerald-400 mt-1">{lang === 'ro' ? 'Fluid' : 'Fluid (< 5m)'}</p>
          </div>
        </div>
      </div>

      {/* Control Bar & Filters */}
      <div className="max-w-7xl mx-auto mb-6 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        {/* Category Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 md:pb-0 scrollbar-none">
          {CATEGORIES.map((cat) => {
            const Icon = cat.icon;
            const isActive = activeCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => setActiveCategory(cat.id)}
                className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-label-sm text-xs transition-all duration-200 whitespace-nowrap border ${
                  isActive
                    ? 'bg-primary/20 text-primary border-primary/50 shadow-[0_0_15px_rgba(209,188,255,0.3)]'
                    : 'bg-surface-container-high/60 text-white/70 border-white/10 hover:bg-white/10 hover:text-white'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{cat.label[lang]}</span>
              </button>
            );
          })}
        </div>

        {/* Search & Map Options */}
        <div className="flex items-center gap-3">
          <div className="relative flex-1 md:w-64">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-white/40" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={lang === 'ro' ? 'Caută scenă, bar, toalete...' : 'Search stage, bar, gate...'}
              className="w-full pl-10 pr-4 py-2 rounded-xl bg-surface-container-high/80 border border-white/10 text-xs text-white placeholder:text-white/40 focus:outline-none focus:border-primary/50 transition-all"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <button
            onClick={() => setShowWaitTimeBadge(!showWaitTimeBadge)}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs border transition-all ${
              showWaitTimeBadge
                ? 'bg-tertiary/20 text-tertiary border-tertiary/40'
                : 'bg-surface-container-high/60 text-white/50 border-white/10'
            }`}
            title={lang === 'ro' ? 'Comută afișarea timpilor de așteptare' : 'Toggle wait time badges'}
          >
            <Clock className="w-4 h-4" />
            <span className="hidden sm:inline">{lang === 'ro' ? 'Timp Așteptare' : 'Wait Badges'}</span>
          </button>
        </div>
      </div>

      {/* Main Interactive Map Workspace */}
      <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Map Container (3 columns on large screens) */}
        <div className="lg:col-span-3 relative rounded-2xl overflow-hidden border border-white/15 bg-surface-container-high/90 shadow-[0_10px_40px_rgba(0,0,0,0.5)] min-h-[550px] flex flex-col">
          {/* Zoom controls floating */}
          <div className="absolute top-4 right-4 z-30 flex flex-col gap-1.5 bg-surface-container-high/90 border border-white/15 p-1.5 rounded-xl backdrop-blur-md shadow-lg">
            <button
              onClick={() => setZoomLevel((z) => Math.min(z + 0.2, 1.8))}
              className="p-2 hover:bg-white/10 rounded-lg text-white transition-colors"
              title="Zoom In"
            >
              <ZoomIn className="w-4 h-4" />
            </button>
            <button
              onClick={() => setZoomLevel((z) => Math.max(z - 0.2, 0.8))}
              className="p-2 hover:bg-white/10 rounded-lg text-white transition-colors"
              title="Zoom Out"
            >
              <ZoomOut className="w-4 h-4" />
            </button>
            <button
              onClick={() => setZoomLevel(1)}
              className="p-2 hover:bg-white/10 rounded-lg text-white transition-colors text-xs font-mono"
              title="Reset Zoom"
            >
              100%
            </button>
          </div>

          {/* Map canvas container */}
          <div className="relative flex-1 w-full h-full overflow-auto p-4 flex items-center justify-center min-h-[500px] bg-slate-950/80">
            <div
              className="relative w-full max-w-4xl aspect-[16/10] rounded-xl overflow-hidden shadow-2xl transition-transform duration-300 origin-center"
              style={{ transform: `scale(${zoomLevel})` }}
            >
              {/* Map background image or fallback stylized SVG blueprint */}
              {mapLayout?.imageUrl ? (
                <img
                  src={mapLayout.imageUrl.startsWith('/') ? `http://localhost:8080${mapLayout.imageUrl}` : mapLayout.imageUrl}
                  alt="Untold Venue Map"
                  className="w-full h-full object-cover select-none"
                />
              ) : (
                <div className="w-full h-full bg-gradient-to-br from-slate-900 via-indigo-950 to-purple-950 relative overflow-hidden flex items-center justify-center border border-white/10">
                  {/* Grid lines styling */}
                  <div
                    className="absolute inset-0 opacity-20 pointer-events-none"
                    style={{
                      backgroundImage:
                        'linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)',
                      backgroundSize: '40px 40px',
                    }}
                  />
                  {/* Decorative arena center circle */}
                  <div className="w-64 h-64 rounded-full border border-primary/20 animate-pulse flex items-center justify-center">
                    <div className="w-40 h-40 rounded-full border border-tertiary/30" />
                  </div>
                  <span className="absolute bottom-4 left-4 text-xs font-mono text-white/30 uppercase tracking-widest">
                    Untold Arena Map Grid (Default Blueprint)
                  </span>
                </div>
              )}

              {loading && (
                <div className="absolute inset-0 bg-slate-950/70 backdrop-blur-sm z-50 flex items-center justify-center">
                  <RefreshCw className="w-8 h-8 text-primary animate-spin" />
                </div>
              )}

              {/* Render Pins with Color-Coded Heatmap Halos */}
              {filteredPins.map((pin) => {
                const PinIcon = getPinIcon(pin.type);
                const level = getPinLevel(pin.id);
                const meta = DENSITY_META[level];
                const isSelected = selectedPin?.id === pin.id;
                const estimatedWaitStr = getEstimatedWait(pin);

                return (
                  <div
                    key={pin.id}
                    onClick={() => setSelectedPin(pin)}
                    className="absolute -translate-x-1/2 -translate-y-1/2 z-20 cursor-pointer group transition-all duration-200"
                    style={{ left: `${pin.xpos}%`, top: `${pin.ypos}%` }}
                  >
                    {/* Glowing Heatmap Pulse Ring */}
                    <div
                      className="absolute inset-0 rounded-full animate-ping pointer-events-none"
                      style={{
                        backgroundColor: meta.color,
                        opacity: level === 'CRITICAL' ? 0.7 : level === 'HIGH' ? 0.4 : 0,
                        transform: 'scale(1.8)',
                      }}
                    />

                    {/* Pin Outer Ring */}
                    <div
                      className={`relative flex items-center justify-center p-2 rounded-full backdrop-blur-md transition-all duration-300 ${
                        isSelected ? 'scale-125 z-30 shadow-[0_0_25px_rgba(255,255,255,0.8)]' : 'group-hover:scale-110'
                      }`}
                      style={{
                        backgroundColor: meta.bgColor,
                        border: `2px solid ${meta.color}`,
                        boxShadow: `0 0 15px ${meta.color}40`,
                      }}
                    >
                      <PinIcon className="w-4 h-4" style={{ color: meta.color }} />
                    </div>

                    {/* Wait Time Floating Badge */}
                    {showWaitTimeBadge && (
                      <div
                        className="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap px-2 py-0.5 rounded-full text-[10px] font-semibold font-mono border backdrop-blur-md shadow-md pointer-events-none transition-all duration-200"
                        style={{
                          backgroundColor: meta.badgeBg,
                          borderColor: meta.borderColor,
                          color: '#FFFFFF',
                        }}
                      >
                        {estimatedWaitStr}
                      </div>
                    )}

                    {/* Pin Label on Hover */}
                    <div className="absolute top-8 left-1/2 -translate-x-1/2 hidden group-hover:flex flex-col items-center bg-slate-900/95 border border-white/20 p-2 rounded-xl text-center whitespace-nowrap z-40 shadow-2xl backdrop-blur-xl pointer-events-none">
                      <span className="text-xs font-bold text-white">{pin.name}</span>
                      <span className="text-[10px] font-mono" style={{ color: meta.color }}>
                        {meta.label[lang]}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Footer Legend */}
          <div className="p-4 border-t border-white/10 bg-surface-container-high/50 flex flex-wrap items-center justify-between gap-4 text-xs">
            <div className="flex items-center gap-4 flex-wrap">
              <span className="text-white/60 font-semibold">{lang === 'ro' ? 'Legență Aglomerație:' : 'Crowd Legend:'}</span>
              {(Object.keys(DENSITY_META) as DensityLevel[]).map((lvl) => (
                <div key={lvl} className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-full" style={{ backgroundColor: DENSITY_META[lvl].color }} />
                  <span className="text-white/80">{DENSITY_META[lvl].label[lang]}</span>
                </div>
              ))}
            </div>
            <span className="text-white/40 text-[11px]">
              {lang === 'ro' ? 'Apasă pe orice pin pentru detalii complete' : 'Click any pin to inspect estimated line queue'}
            </span>
          </div>
        </div>

        {/* Pin Details Drawer Sidebar (1 column) */}
        <div className="lg:col-span-1 flex flex-col gap-4">
          {selectedPin ? (
            <div className="p-6 rounded-2xl bg-surface-container-high border border-white/15 backdrop-blur-xl shadow-2xl animate-in fade-in slide-in-from-right duration-200">
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-2.5">
                  {React.createElement(getPinIcon(selectedPin.type), {
                    className: 'w-6 h-6',
                    style: { color: DENSITY_META[getPinLevel(selectedPin.id)].color },
                  })}
                  <div>
                    <h3 className="font-bold text-lg text-white leading-snug">{selectedPin.name}</h3>
                    <span className="text-xs text-white/50 font-mono uppercase">{selectedPin.type}</span>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedPin(null)}
                  className="text-white/40 hover:text-white p-1 rounded-lg hover:bg-white/10"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <p className="text-xs text-white/70 mb-5 leading-relaxed bg-white/5 p-3 rounded-xl border border-white/5">
                {selectedPin.description || (lang === 'ro' ? 'Punct de interes pe harta Untold.' : 'Official venue point of interest.')}
              </p>

              {/* Crowd & Wait Time Info Card */}
              {(() => {
                const level = getPinLevel(selectedPin.id);
                const meta = DENSITY_META[level];
                const density = getPinDensity(selectedPin.id);
                const isService = ['FOOD_COURT', 'BAR', 'CREDIT_POINT', 'TOILET', 'ENTRANCE'].includes(selectedPin.type);

                return (
                  <div
                    className="p-4 rounded-xl border mb-5"
                    style={{
                      backgroundColor: meta.bgColor,
                      borderColor: meta.borderColor,
                    }}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-semibold uppercase tracking-wider text-white/80">
                        {lang === 'ro' ? 'Nivel Aglomerație' : 'Density Status'}
                      </span>
                      <span
                        className="px-2.5 py-0.5 rounded-full text-xs font-bold font-mono"
                        style={{ backgroundColor: meta.badgeBg, color: meta.color }}
                      >
                        {meta.label[lang]}
                      </span>
                    </div>

                    <div className="mt-3 flex items-center gap-3">
                      <Clock className="w-5 h-5 text-white/80 shrink-0" />
                      <div>
                        <p className="text-xs text-white/60">
                          {isService
                            ? lang === 'ro'
                              ? 'Timp estimat de așteptare la coadă:'
                              : 'Estimated queue wait time:'
                            : lang === 'ro'
                            ? 'Stare capacitate zonă:'
                            : 'Stage perimeter density:'}
                        </p>
                        <p className="text-base font-bold text-white mt-0.5">{getEstimatedWait(selectedPin)}</p>
                      </div>
                    </div>

                    {density && (
                      <div className="mt-3 pt-3 border-t border-white/10 text-[11px] text-white/60 flex items-center justify-between">
                        <span>
                          {density.reportCount} {lang === 'ro' ? 'rapoarte staff' : 'staff reports'}
                        </span>
                        <span>
                          {lang === 'ro' ? 'Ultimul: ' : 'Updated: '}
                          {new Date(density.lastReportedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                    )}
                  </div>
                );
              })()}

              <div className="space-y-2">
                <div className="p-3 rounded-xl bg-white/5 border border-white/5 flex items-center gap-3 text-xs text-white/80">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>{lang === 'ro' ? 'Plată contactless & brățară acceptată' : 'Contactless & wristband wallet accepted'}</span>
                </div>
                <div className="p-3 rounded-xl bg-white/5 border border-white/5 flex items-center gap-3 text-xs text-white/80">
                  <Info className="w-4 h-4 text-primary shrink-0" />
                  <span>{lang === 'ro' ? 'Date actualizate în timp real' : 'Live crowd feed auto-synced every 30s'}</span>
                </div>
              </div>
            </div>
          ) : (
            <div className="p-6 rounded-2xl bg-surface-container-high/60 border border-white/10 backdrop-blur-xl flex flex-col items-center justify-center text-center text-white/50 min-h-[300px]">
              <Compass className="w-12 h-12 text-primary/40 mb-3 animate-pulse" />
              <h4 className="font-bold text-white text-base mb-1">
                {lang === 'ro' ? 'Selectează un pin de pe hartă' : 'Select a Pin on the Map'}
              </h4>
              <p className="text-xs text-white/50 max-w-xs">
                {lang === 'ro'
                  ? 'Apasă pe orice bar, scenă sau punct de utilități pentru a vedea timpul estimat de așteptare.'
                  : 'Click any stage, food truck, bar, or entrance gate to inspect real-time crowd congestion.'}
              </p>
            </div>
          )}
        </div>
      </div>
    </main>
  );
};

export default VenueMap;
