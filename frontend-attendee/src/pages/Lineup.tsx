import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '../context/I18nContext';
import { useAuth } from '../context/AuthContext';
import { useSchedule } from '../context/ScheduleContext';

interface SongDto {
  id: number;
  title: string;
  durationSeconds: number;
  spotifyId: string | null;
  previewUrl: string | null;
}

interface Artist {
  id: number;
  name: string;
  genre: string | null;
  biography: string | null;
  imageUrl: string | null;
  spotifyId: string | null;
  songs?: SongDto[] | null;
}

interface Performance {
  id: number;
  artistId: number;
  artistName: string;
  artistImageUrl: string | null;
  stageId: number;
  stageName: string;
  startTime: string; // ISO OffsetDateTime
  endTime: string;   // ISO OffsetDateTime
  status: 'SCHEDULED' | 'LIVE' | 'DELAYED' | 'CANCELLED' | 'COMPLETED';
  hasConflict: boolean;
}

interface StagePin {
  id: number;
  type: string;
  name: string;
  description: string | null;
  xpos: number;
  ypos: number;
}

const FALLBACK_IMAGE = '/assets/mainstage.png';

// Festival day 1 = Aug 6; used to map a schedule performance to the personal-schedule day (1-4).
const DAY_ID_TO_NUMBER: Record<string, number> = {
  'Day 1': 1,
  'Day 2': 2,
  'Day 3': 3,
  'Day 4': 4,
};

interface ScheduleToggleButtonProps {
  artistName: string;
  stageName: string;
  startTime: string; // "HH:MM"
  endTime: string;   // "HH:MM"
  festivalDay: number;
  variant?: 'card' | 'full';
}

/**
 * Toggle that adds/removes a performance from the attendee's personal schedule (the My Schedule
 * feature — clash detection + alarms live there). Reused on the lineup cards and in the set overlay.
 */
const ScheduleToggleButton: React.FC<ScheduleToggleButtonProps> = ({
  artistName, stageName, startTime, endTime, festivalDay, variant = 'full',
}) => {
  const { isScheduled, toggleSlot } = useSchedule();
  const scheduled = isScheduled(artistName, stageName);

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    toggleSlot({ artistName, stageName, startTime, endTime, festivalDay });
  };

  if (variant === 'card') {
    return (
      <button
        onClick={handleClick}
        aria-pressed={scheduled}
        title={scheduled ? 'În programul meu' : 'Adaugă în programul meu'}
        className={`mt-3 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full border font-label-sm text-[10px] uppercase tracking-widest transition-all duration-300 active:scale-95 ${scheduled ? 'border-tertiary text-tertiary bg-tertiary/10 shadow-[0_0_12px_rgba(0,219,233,0.25)]' : 'border-white/20 text-white/80 bg-black/30 backdrop-blur-sm hover:border-primary hover:text-primary'}`}
      >
        <span className="material-symbols-outlined text-[15px]">
          {scheduled ? 'event_available' : 'calendar_add_on'}
        </span>
        <span>{scheduled ? 'În program' : 'Adaugă'}</span>
      </button>
    );
  }

  return (
    <button
      onClick={handleClick}
      aria-pressed={scheduled}
      className={`w-full py-3 px-4 rounded-xl font-label-md uppercase tracking-wider transition-all duration-300 flex items-center justify-center gap-2 font-bold border-2 ${scheduled ? 'border-tertiary text-tertiary bg-tertiary/10 shadow-[0_0_15px_rgba(0,219,233,0.2)]' : 'border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary'}`}
    >
      <span className="material-symbols-outlined text-lg">
        {scheduled ? 'event_available' : 'calendar_add_on'}
      </span>
      <span>{scheduled ? 'În programul meu' : 'Adaugă în programul meu'}</span>
    </button>
  );
};

export const Lineup: React.FC = () => {
  const { t } = useI18n();
  const { user, token } = useAuth();
  const navigate = useNavigate();

  // Primary Lineup Grid State
  const [artists, setArtists] = useState<Artist[]>([]);
  const [loading, setLoading] = useState(true);
  const [favoriteIds, setFavoriteIds] = useState<Set<number>>(new Set());
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [selectedGenre, setSelectedGenre] = useState<string | 'all'>('all');
  const [favoritesOnly, setFavoritesOnly] = useState(false);

  // Tab State ('lineup' | 'schedule' | 'my-schedule')
  const [activeTab, setActiveTab] = useState<'lineup' | 'schedule' | 'my-schedule'>('lineup');

  // Timeline Schedule State
  const [performances, setPerformances] = useState<Performance[]>([]);
  const [stages, setStages] = useState<StagePin[]>([]);
  const [loadingSchedule, setLoadingSchedule] = useState(true);
  const [selectedDay, setSelectedDay] = useState('Day 2');

  // Interactive Set Detail Overlay Popup State
  const [selectedSet, setSelectedSet] = useState<Performance | null>(null);
  const [detailArtist, setDetailArtist] = useState<Artist | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [isPlayingPreview, setIsPlayingPreview] = useState(false);
  const [audioPreviewUrl, setAudioPreviewUrl] = useState<string | null>(null);
  const [audioElement, setAudioElement] = useState<HTMLAudioElement | null>(null);

  const days = [
    { id: 'Day 1', label: t('dayWord') + ' 1', desc: 'Thur, Aug 6' },
    { id: 'Day 2', label: t('dayWord') + ' 2', desc: 'Fri, Aug 7' },
    { id: 'Day 3', label: t('dayWord') + ' 3', desc: 'Sat, Aug 8' },
    { id: 'Day 4', label: t('dayWord') + ' 4', desc: 'Sun, Aug 9' },
  ];

  const logicalStartHour = 16;
  const logicalEndHour = 30; // 06:00 AM next day
  const totalHoursRange = logicalEndHour - logicalStartHour;

  // Polling & Data sync for Schedule Matrix
  const fetchScheduleData = async () => {
    try {
      const mapRes = await fetch('http://localhost:8080/api/map/active');
      const mapData = await mapRes.json();
      let stagePins: StagePin[] = [];
      if (mapData && mapData.type === 'SUCCESS' && mapData.data && mapData.data.pins) {
        stagePins = mapData.data.pins.filter((pin: any) => pin.type.toUpperCase() === 'STAGE');
      }
      setStages(stagePins);

      const perfRes = await fetch('http://localhost:8080/api/performances');
      const perfData = await perfRes.json();
      if (perfData && perfData.type === 'SUCCESS') {
        setPerformances(perfData.data);
      }
    } catch (err) {
      console.error('Failed to sync festival schedule data', err);
    } finally {
      setLoadingSchedule(false);
    }
  };

  useEffect(() => {
    fetchScheduleData();

    // Setup 30-second background polling for live set status and delays
    const interval = setInterval(() => {
      fetchScheduleData();
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  // Fetch Artists list
  useEffect(() => {
    const fetchArtists = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/artists');
        const data = await res.json();
        if (data && data.type === 'SUCCESS') {
          setArtists(data.data);
        }
      } catch (err) {
        console.error('Failed to fetch artists', err);
      } finally {
        setLoading(false);
      }
    };
    fetchArtists();
  }, []);

  // Fetch Favorite IDs
  useEffect(() => {
    if (!user || !token) {
      setFavoriteIds(new Set());
      return;
    }
    const fetchFavorites = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/favorites/me', {
          headers: { Authorization: `Bearer ${token}` },
        });
        const data = await res.json();
        if (data && data.type === 'SUCCESS') {
          setFavoriteIds(new Set(data.data.map((a: Artist) => a.id)));
        }
      } catch (err) {
        console.error('Failed to fetch favorites', err);
      }
    };
    fetchFavorites();
  }, [user, token]);

  const genres = useMemo(() => {
    const unique = Array.from(new Set(artists.map(a => a.genre).filter((g): g is string => Boolean(g))));
    return unique.sort();
  }, [artists]);

  const filteredArtists = artists.filter((artist) => {
    if (selectedGenre !== 'all' && artist.genre !== selectedGenre) return false;
    if (favoritesOnly && !favoriteIds.has(artist.id)) return false;
    return true;
  });

  const handleToggleFavorite = async (artistId: number) => {
    if (!user || !token) {
      navigate('/auth');
      return;
    }
    setPendingId(artistId);
    try {
      const res = await fetch(`http://localhost:8080/api/favorites/${artistId}/toggle`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (data && data.type === 'SUCCESS') {
        setFavoriteIds(prev => {
          const next = new Set(prev);
          if (data.data) next.add(artistId);
          else next.delete(artistId);
          return next;
        });
      }
    } catch (err) {
      console.error('Failed to toggle favorite', err);
    } finally {
      setPendingId(null);
    }
  };

  // Convert ISO string to Europe/Bucharest timezone details
  const getLogicalTimeDetails = (isoStr: string) => {
    try {
      const date = new Date(isoStr);
      const bucarestFormatter = new Intl.DateTimeFormat("en-US", {
        timeZone: "Europe/Bucharest",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
      });

      const parts = bucarestFormatter.formatToParts(date);
      const partMap = Object.fromEntries(parts.map(p => [p.type, p.value]));
      
      const year = partMap.year;
      const month = partMap.month;
      const day = partMap.day;
      const h = parseInt(partMap.hour, 10);
      const m = parseInt(partMap.minute, 10);

      const hh = String(h).padStart(2, '0');
      const mm = String(m).padStart(2, '0');
      const timeStr = `${hh}:${mm}`;

      let logicalDate = new Date(`${year}-${month}-${day}T00:00:00`);
      if (h < 12) {
        logicalDate.setDate(logicalDate.getDate() - 1);
      }

      const lYear = logicalDate.getFullYear();
      const lMonth = String(logicalDate.getMonth() + 1).padStart(2, '0');
      const lDay = String(logicalDate.getDate()).padStart(2, '0');
      const ymd = `${lYear}-${lMonth}-${lDay}`;

      const dateMapping: Record<string, string> = {
        '2026-08-06': 'Day 1',
        '2026-08-07': 'Day 2',
        '2026-08-08': 'Day 3',
        '2026-08-09': 'Day 4',
      };

      return {
        dayId: dateMapping[ymd] || 'Day 1',
        timeStr,
        hour: h < 12 ? h + 24 : h,
        minutes: m
      };
    } catch (e) {
      return { dayId: 'Day 1', timeStr: '18:00', hour: 18, minutes: 0 };
    }
  };

  const getGanttPosition = (startTimeStr: string, endTimeStr: string) => {
    const startDetails = getLogicalTimeDetails(startTimeStr);
    const endDetails = getLogicalTimeDetails(endTimeStr);

    const startDecimal = startDetails.hour + (startDetails.minutes / 60);
    const endDecimal = endDetails.hour + (endDetails.minutes / 60);

    const relativeStart = Math.max(logicalStartHour, Math.min(logicalEndHour, startDecimal));
    const relativeEnd = Math.max(logicalStartHour, Math.min(logicalEndHour, endDecimal));

    const left = ((relativeStart - logicalStartHour) / totalHoursRange) * 100;
    const width = ((relativeEnd - relativeStart) / totalHoursRange) * 100;

    return {
      left: `${left}%`,
      width: `${width}%`
    };
  };

  // The earliest performance for an artist, so a lineup card knows which set to add to the
  // personal schedule (start/end/stage/day). Null if the artist has no scheduled performance.
  const getPrimaryPerformance = (artistId: number): Performance | null => {
    const perfs = performances
      .filter(p => p.artistId === artistId)
      .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    return perfs[0] ?? null;
  };

  // Automated Clashes (Overlaps) Detector for favorited sets
  const favoritedClashes = useMemo(() => {
    const dayFavorites = performances.filter(p => 
      favoriteIds.has(p.artistId) && 
      getLogicalTimeDetails(p.startTime).dayId === selectedDay
    );

    const clashes: { p1: Performance; p2: Performance }[] = [];
    for (let i = 0; i < dayFavorites.length; i++) {
      for (let j = i + 1; j < dayFavorites.length; j++) {
        const s1 = new Date(dayFavorites[i].startTime).getTime();
        const e1 = new Date(dayFavorites[i].endTime).getTime();
        const s2 = new Date(dayFavorites[j].startTime).getTime();
        const e2 = new Date(dayFavorites[j].endTime).getTime();

        if (s1 < e2 && e1 > s2) {
          clashes.push({ p1: dayFavorites[i], p2: dayFavorites[j] });
        }
      }
    }
    return clashes;
  }, [performances, favoriteIds, selectedDay]);

  const myScheduleClashIds = useMemo(() => {
    const favs = performances.filter(p => favoriteIds.has(p.artistId));
    const clashes = new Set<number>();
    for (let i = 0; i < favs.length; i++) {
      for (let j = i + 1; j < favs.length; j++) {
        if (getLogicalTimeDetails(favs[i].startTime).dayId !== getLogicalTimeDetails(favs[j].startTime).dayId) continue;
        const s1 = new Date(favs[i].startTime).getTime();
        const e1 = new Date(favs[i].endTime).getTime();
        const s2 = new Date(favs[j].startTime).getTime();
        const e2 = new Date(favs[j].endTime).getTime();
        if (s1 < e2 && e1 > s2) {
          clashes.add(favs[i].id);
          clashes.add(favs[j].id);
        }
      }
    }
    return clashes;
  }, [performances, favoriteIds]);

  const myScheduleGrouped = useMemo(() => {
    const favs = performances.filter(p => favoriteIds.has(p.artistId));
    const grouped = [
      { id: 'Day 1', performances: [] as Performance[] },
      { id: 'Day 2', performances: [] as Performance[] },
      { id: 'Day 3', performances: [] as Performance[] },
      { id: 'Day 4', performances: [] as Performance[] },
    ];
    favs.forEach(p => {
      const dayId = getLogicalTimeDetails(p.startTime).dayId;
      const group = grouped.find(g => g.id === dayId);
      if (group) group.performances.push(p);
    });
    grouped.forEach(g => {
      g.performances.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    });
    return grouped;
  }, [performances, favoriteIds]);

  // Handle set block clicking to fetch detailed profile & playlist audio previews
  const handleOpenSetDetails = async (perf: Performance) => {
    setSelectedSet(perf);
    setDetailArtist(null);
    setLoadingDetail(true);
    setIsPlayingPreview(false);
    setAudioPreviewUrl(null);
    
    if (audioElement) {
      audioElement.pause();
    }

    try {
      const res = await fetch(`http://localhost:8080/api/artists/${perf.artistId}`);
      const data = await res.json();
      if (data && data.type === 'SUCCESS' && data.data) {
        const artist: Artist = data.data;
        setDetailArtist(artist);
        
        // Find if they have any synced audio previews
        if (artist.songs && artist.songs.length > 0) {
          const previewSong = artist.songs.find(s => s.previewUrl);
          if (previewSong && previewSong.previewUrl) {
            setAudioPreviewUrl(previewSong.previewUrl);
            const audio = new Audio(previewSong.previewUrl);
            setAudioElement(audio);
            audio.onended = () => setIsPlayingPreview(false);
          }
        }
      }
    } catch (err) {
      console.error("Failed to fetch artist details", err);
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleCloseDetails = () => {
    setSelectedSet(null);
    setDetailArtist(null);
    setIsPlayingPreview(false);
    if (audioElement) {
      audioElement.pause();
    }
  };

  const toggleAudioPlayback = () => {
    if (!audioElement) return;
    if (isPlayingPreview) {
      audioElement.pause();
      setIsPlayingPreview(false);
    } else {
      audioElement.play().catch(err => console.warn("Audio blocked by browser autoplay", err));
      setIsPlayingPreview(true);
    }
  };

  useEffect(() => {
    return () => {
      if (audioElement) {
        audioElement.pause();
      }
    };
  }, [audioElement]);

  return (
    <main className="flex-grow pt-[100px] md:pt-[120px] pb-xl px-margin-mobile md:px-margin-desktop animate-fade-in text-white">

      {/* Tab Switcher - Artists Grid vs Schedule Timeline */}
      <section className="mb-xl flex flex-col md:flex-row md:items-center md:justify-between border-b border-white/10 pb-4 gap-6">
        <div>
          <h1 className="font-display-xl-mobile md:font-display-xl text-display-xl-mobile md:text-display-xl uppercase text-primary mb-1 text-glow">
            {t('theLineup')}
          </h1>
          <p className="text-outline-variant text-sm font-medium">
            {activeTab === 'lineup' ? 'Explore our legendary list of musical dreamers.' : activeTab === 'schedule' ? t('scheduleDesc') : 'Your personalized festival itinerary.'}
          </p>
        </div>

        <div className="flex bg-background/50 backdrop-blur-md border border-white/10 p-1.5 rounded-2xl w-fit">
          <button
            onClick={() => setActiveTab('lineup')}
            className={`px-6 py-2.5 rounded-xl font-headline-sm uppercase transition-all flex items-center gap-2 ${activeTab === 'lineup' ? 'bg-primary text-white font-bold shadow-[0_0_15px_rgba(112,0,255,0.4)]' : 'text-outline-variant hover:text-white'}`}
          >
            <span className="material-symbols-outlined text-lg">grid_view</span>
            <span>{t('theLineup')}</span>
          </button>
          <button
            onClick={() => setActiveTab('schedule')}
            className={`px-6 py-2.5 rounded-xl font-headline-sm uppercase transition-all flex items-center gap-2 ${activeTab === 'schedule' ? 'bg-primary text-white font-bold shadow-[0_0_15px_rgba(112,0,255,0.4)]' : 'text-outline-variant hover:text-white'}`}
          >
            <span className="material-symbols-outlined text-lg">calendar_month</span>
            <span>Schedule</span>
          </button>
          <button
            onClick={() => setActiveTab('my-schedule')}
            className={`px-6 py-2.5 rounded-xl font-headline-sm uppercase transition-all flex items-center gap-2 ${activeTab === 'my-schedule' ? 'bg-primary text-white font-bold shadow-[0_0_15px_rgba(112,0,255,0.4)]' : 'text-outline-variant hover:text-white'}`}
          >
            <span className="material-symbols-outlined text-lg">event_available</span>
            <span>My Schedule</span>
          </button>
        </div>
      </section>

      {/* RENDER TAB 1: ARTISTS GRID */}
      {activeTab === 'lineup' && (
        <>
          <section className="mb-8">
            <div className="flex flex-wrap justify-center md:justify-start gap-4">
              <button
                onClick={() => setSelectedGenre('all')}
                className={`px-4 py-2 rounded border font-label-sm uppercase transition-colors ${selectedGenre === 'all' ? 'border-primary text-primary bg-primary/10' : 'border-outline text-outline hover:border-primary hover:text-primary'}`}>
                {t('allGenres')}
              </button>
              {genres.map((genre) => (
                <button
                  key={genre}
                  onClick={() => setSelectedGenre(genre)}
                  className={`px-4 py-2 rounded border font-label-sm uppercase transition-colors ${selectedGenre === genre ? 'border-primary text-primary bg-primary/10' : 'border-outline text-outline hover:border-primary hover:text-primary'}`}>
                  {genre}
                </button>
              ))}
              <button
                onClick={() => setFavoritesOnly(prev => !prev)}
                className={`px-4 py-2 rounded-full border-2 font-label-sm uppercase tracking-widest transition-colors flex items-center gap-2 ${favoritesOnly ? 'border-tertiary text-tertiary bg-tertiary/10 shadow-[0_0_15px_rgba(0,219,233,0.3)]' : 'border-outline-variant text-on-surface-variant hover:border-tertiary hover:text-tertiary glass-panel'}`}>
                <span className="material-symbols-outlined text-base">
                  {favoritesOnly ? 'favorite' : 'favorite_border'}
                </span>
                {t('favoritesOnly')}
              </button>
            </div>
          </section>

          <section>
            {loading ? (
              <div className="text-center text-outline-variant py-xl">{t('loadingArtists')}</div>
            ) : filteredArtists.length === 0 ? (
              <div className="text-center text-outline-variant py-xl">
                {favoritesOnly && !user ? t('loginToFavorite') : favoritesOnly ? t('noFavoritesYet') : t('noArtists')}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-gutter animate-fade-in">
                {filteredArtists.map((artist) => {
                  const isFavorite = favoriteIds.has(artist.id);
                  return (
                    <article
                      key={artist.id}
                      className="group relative overflow-hidden rounded-xl glass-panel neon-border-hover transition-all duration-300 h-[320px] shadow-[0_0_20px_rgba(255,255,255,0.15)] hover:shadow-[0_0_30px_rgba(255,255,255,0.3)]"
                    >
                      <div
                        className="absolute inset-0 bg-cover bg-center opacity-50 group-hover:opacity-70 transition-opacity duration-500 chromatic-hover"
                        style={{ backgroundImage: `url('${artist.imageUrl || FALLBACK_IMAGE}')` }}
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent" />

                      <button
                        onClick={() => handleToggleFavorite(artist.id)}
                        disabled={pendingId === artist.id}
                        aria-label="Toggle favorite"
                        className="absolute top-4 right-4 z-10 w-10 h-10 rounded-full bg-black/40 backdrop-blur-sm border border-white/10 flex items-center justify-center hover:bg-black/60 transition-all disabled:opacity-50"
                      >
                        <span className={`material-symbols-outlined text-xl ${isFavorite ? 'text-tertiary' : 'text-white'}`}>
                          {isFavorite ? 'favorite' : 'favorite_border'}
                        </span>
                      </button>

                      <div className="absolute bottom-0 left-0 p-lg w-full">
                        {artist.genre && (
                          <span className="inline-block px-3 py-1 mb-3 text-xs font-label-sm uppercase tracking-widest bg-primary-container text-on-primary-container rounded shadow-[0_0_10px_rgba(112,0,255,0.5)]">
                            {artist.genre}
                          </span>
                        )}
                        <h2 className="font-headline-lg-mobile text-headline-lg-mobile uppercase text-white group-hover:text-tertiary transition-colors duration-300">
                          {artist.name}
                        </h2>
                        {(() => {
                          const perf = getPrimaryPerformance(artist.id);
                          if (!perf) return null;
                          const start = getLogicalTimeDetails(perf.startTime);
                          const end = getLogicalTimeDetails(perf.endTime);
                          return (
                            <ScheduleToggleButton
                              artistName={perf.artistName}
                              stageName={perf.stageName}
                              startTime={start.timeStr}
                              endTime={end.timeStr}
                              festivalDay={DAY_ID_TO_NUMBER[start.dayId] ?? 1}
                              variant="card"
                            />
                          );
                        })()}
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        </>
      )}

      {/* RENDER TAB 2: SCHEDULE TIMELINE */}
      {activeTab === 'schedule' && (
        <div className="animate-fade-in flex flex-col gap-6">

          {/* Controls Bar: Day navigation, live sync indicator, refresh */}
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 glass-panel p-4 rounded-2xl border border-white/10">
            {/* Days Tabs */}
            <div className="flex bg-background/50 border border-white/5 p-1 rounded-xl gap-1 overflow-x-auto">
              {days.map((day) => (
                <button
                  key={day.id}
                  onClick={() => setSelectedDay(day.id)}
                  className={`px-4 py-2 rounded-lg font-label-sm uppercase transition-all flex flex-col items-center min-w-[90px] ${selectedDay === day.id ? 'bg-primary text-white font-bold' : 'text-outline-variant hover:text-white'}`}
                >
                  <span className="text-xs font-bold tracking-wider">{day.label}</span>
                  <span className="text-[9.5px] opacity-75 mt-0.5">{day.desc}</span>
                </button>
              ))}
            </div>

            {/* Live Indicator & Refresher */}
            <div className="flex items-center gap-4 justify-between md:justify-end">
              {/* Green Pulsing Live Sync Dot */}
              <div className="flex items-center gap-2.5 px-4 py-2 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 text-xs font-bold uppercase tracking-widest">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
                <span className="w-2 h-2 rounded-full bg-emerald-400 absolute" />
                <span>Live Syncing</span>
              </div>

              <button 
                onClick={fetchScheduleData}
                className="w-10 h-10 rounded-xl bg-background/50 border border-white/10 flex items-center justify-center text-outline-variant hover:text-white transition-all"
                title="Force refresh lineup times"
              >
                <span className="material-symbols-outlined text-lg">sync</span>
              </button>
            </div>
          </div>

          {/* Warnings Bar: Starred Clashes Notification */}
          {favoritedClashes.length > 0 && (
            <div className="glass-panel p-4 rounded-xl border border-coral-accent/20 bg-coral-accent/10 flex gap-4 items-center animate-pulse shadow-[0_0_20px_rgba(255,111,97,0.15)]">
              <span className="material-symbols-outlined text-coral-accent text-3xl">warning</span>
              <div>
                <h5 className="font-bold text-coral-accent text-sm uppercase tracking-wider">{t('clashesAlert')}</h5>
                <p className="text-xs text-white/80 mt-1">
                  You have overlapping sets booked! {favoritedClashes.map((c, idx) => (
                    <span key={idx} className="block mt-1 font-medium">
                      ⚠️ <strong>{c.p1.artistName}</strong> ({c.p1.stageName}) conflicts with <strong>{c.p2.artistName}</strong> ({c.p2.stageName}) on {getLogicalTimeDetails(c.p1.startTime).timeStr}.
                    </span>
                  ))}
                </p>
              </div>
            </div>
          )}

          {/* Horizontal Gantt Matrix Grid */}
          <div className="glass-panel p-6 rounded-2xl border border-white/10 overflow-x-auto shadow-2xl">
            <div className="min-w-[850px] flex flex-col gap-4 relative">
              
              {/* Matrix Time Markers Header Row (16:00 to 06:00) */}
              <div className="flex border-b border-white/5 pb-3 items-center">
                <div className="w-[150px] flex-shrink-0" />
                <div className="flex-1 flex justify-between font-label-sm text-outline-variant text-[11px] font-bold tracking-widest">
                  <span>16:00</span>
                  <span>18:00</span>
                  <span>20:00</span>
                  <span>22:00</span>
                  <span>00:00</span>
                  <span>02:00</span>
                  <span>04:00</span>
                  <span>06:00</span>
                </div>
              </div>

              {loadingSchedule ? (
                <div className="text-center py-xl text-outline-variant flex items-center justify-center gap-3">
                  <span className="material-symbols-outlined animate-spin">sync</span>
                  <span>Synchronizing timetable matrix...</span>
                </div>
              ) : stages.length === 0 ? (
                <div className="text-center py-xl text-outline-variant">
                  <span className="material-symbols-outlined text-4xl mb-3 block opacity-50">map</span>
                  <strong>No Active Map Stages Configured</strong>
                  <p className="text-xs mt-1">All festival stages are generated dynamically from map pins. Check back soon!</p>
                </div>
              ) : (
                stages.map((stage) => {
                  const stagePerfs = performances.filter(p => 
                    p.stageId === stage.id && 
                    getLogicalTimeDetails(p.startTime).dayId === selectedDay
                  );

                  // If stage is empty of performances for selected day, let's auto-hide row to keep timeline clean
                  if (stagePerfs.length === 0) return null;

                  return (
                    <div key={stage.id} className="flex h-14 items-center bg-background/20 hover:bg-background/40 border border-white/5 rounded-xl transition-all duration-300">
                      {/* Left Header: Stage Label */}
                      <div className="w-[150px] flex-shrink-0 pl-4 font-headline-sm uppercase text-white tracking-wider truncate font-semibold border-r border-white/5 h-full flex items-center bg-background/30 rounded-l-xl">
                        {stage.name}
                      </div>

                      {/* Right Body: Timeline slots slider area */}
                      <div className="flex-grow h-full relative border-l border-white/5">
                        {stagePerfs.map((perf) => {
                          const pos = getGanttPosition(perf.startTime, perf.endTime);
                          const isFav = favoriteIds.has(perf.artistId);

                          // Style according to status and favoritism
                          let themeStyle = "bg-primary/80 border-primary shadow-[0_0_15px_rgba(112,0,255,0.3)]";
                          if (perf.status === 'CANCELLED') themeStyle = "bg-red-500/80 border-red-500 shadow-[0_0_15px_rgba(239,68,68,0.3)]";
                          else if (perf.status === 'DELAYED') themeStyle = "bg-amber-500/80 border-amber-500 shadow-[0_0_15px_rgba(245,158,11,0.3)]";
                          else if (perf.status === 'LIVE') themeStyle = "bg-emerald-500/80 border-emerald-400 shadow-[0_0_20px_rgba(16,185,129,0.5)] border-2 animate-pulse";

                          return (
                            <div
                              key={perf.id}
                              onClick={() => handleOpenSetDetails(perf)}
                              style={{ left: pos.left, width: pos.width }}
                              className={`absolute top-2 bottom-2 rounded-lg border flex flex-col justify-center px-3 cursor-pointer transition-all duration-300 hover:scale-[1.02] hover:z-20 ${themeStyle}`}
                            >
                              <div className="flex items-center justify-between gap-1 w-full overflow-hidden">
                                <span className="text-[11.5px] font-bold tracking-wide truncate uppercase text-white flex items-center gap-1">
                                  {isFav && <span className="material-symbols-outlined text-[12px] text-tertiary">favorite</span>}
                                  {perf.artistName}
                                </span>
                                {perf.status === 'LIVE' && (
                                  <span className="text-[7.5px] bg-red-600 px-1 py-0.5 rounded font-black text-white animate-pulse">LIVE</span>
                                )}
                              </div>
                              <span className="text-[9px] opacity-80 font-semibold tracking-widest mt-0.5">
                                {getLogicalTimeDetails(perf.startTime).timeStr} - {getLogicalTimeDetails(perf.endTime).timeStr}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Grid Footer Legend */}
          <div className="flex flex-wrap justify-between items-center text-xs text-outline-variant gap-4 px-2">
            <div className="flex flex-wrap gap-5 font-semibold uppercase tracking-wider">
              <div className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-full bg-primary shadow-[0_0_10px_rgba(112,0,255,0.4)]" /><span>Scheduled</span></div>
              <div className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping shadow-[0_0_10px_rgba(16,185,129,0.4)]" /><span>Performing Now (Live)</span></div>
              <div className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-full bg-amber-500 shadow-[0_0_10px_rgba(245,158,11,0.4)]" /><span>Delayed / Shifted</span></div>
              <div className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-full bg-red-500 shadow-[0_0_10px_rgba(239,68,68,0.4)]" /><span>Cancelled</span></div>
            </div>
            <div className="italic font-medium">Tip: Star artists in the Lineup to highlight them in favorite gold on this schedule.</div>
          </div>
        </div>
      )}

      {/* RENDER TAB 3: MY SCHEDULE */}
      {activeTab === 'my-schedule' && (
        <div className="animate-fade-in flex flex-col gap-8 max-w-4xl mx-auto w-full">
          {!user ? (
            <div className="glass-panel p-12 rounded-3xl text-center border border-white/5 flex flex-col items-center justify-center">
              <span className="material-symbols-outlined text-5xl text-primary mb-4">lock</span>
              <h2 className="text-2xl font-bold uppercase mb-2">Login Required</h2>
              <p className="text-outline-variant mb-6">Please log in to manage and view your personalized festival itinerary.</p>
              <button onClick={() => navigate('/auth')} className="bg-primary hover:bg-primary/80 text-white font-bold uppercase px-8 py-3 rounded-xl transition-all shadow-[0_0_15px_rgba(112,0,255,0.4)]">Login Now</button>
            </div>
          ) : favoriteIds.size === 0 ? (
            <div className="glass-panel p-12 rounded-3xl text-center border border-white/5 flex flex-col items-center justify-center">
              <span className="material-symbols-outlined text-5xl text-outline-variant mb-4">event_note</span>
              <h2 className="text-2xl font-bold uppercase mb-2">Your Schedule is Empty</h2>
              <p className="text-outline-variant mb-6">Star your favorite artists in the Lineup to build your personalized itinerary.</p>
              <button onClick={() => setActiveTab('lineup')} className="bg-primary hover:bg-primary/80 text-white font-bold uppercase px-8 py-3 rounded-xl transition-all shadow-[0_0_15px_rgba(112,0,255,0.4)]">Explore Lineup</button>
            </div>
          ) : (
            myScheduleGrouped.map((group) => {
              if (group.performances.length === 0) return null;
              const dayInfo = days.find(d => d.id === group.id);
              return (
                <div key={group.id} className="flex flex-col gap-4">
                  <div className="flex items-center gap-4">
                    <h3 className="font-display-md text-2xl uppercase font-bold text-tertiary text-glow">{dayInfo?.label || group.id}</h3>
                    <span className="text-sm font-medium text-outline-variant uppercase tracking-widest">{dayInfo?.desc}</span>
                    <div className="flex-1 h-px bg-gradient-to-r from-tertiary/50 to-transparent"></div>
                  </div>
                  <div className="grid grid-cols-1 gap-4">
                    {group.performances.map((perf) => {
                      const isClashing = myScheduleClashIds.has(perf.id);
                      return (
                        <div 
                          key={perf.id} 
                          className={`glass-panel p-4 rounded-xl border-l-4 flex flex-col md:flex-row md:items-center justify-between gap-4 transition-all hover:bg-white/5 ${isClashing ? 'border-l-coral-accent shadow-[0_0_15px_rgba(255,111,97,0.15)] bg-coral-accent/5' : 'border-l-primary shadow-[0_0_10px_rgba(112,0,255,0.1)]'}`}
                        >
                          <div className="flex items-center gap-4 cursor-pointer flex-1" onClick={() => handleOpenSetDetails(perf)}>
                            <div className="w-16 h-16 rounded-lg bg-cover bg-center shrink-0 border border-white/10" style={{ backgroundImage: `url('${perf.artistImageUrl || FALLBACK_IMAGE}')` }} />
                            <div className="flex flex-col">
                              <span className="text-[10px] font-bold text-outline-variant uppercase tracking-widest">{perf.stageName}</span>
                              <h4 className="text-lg font-bold text-white uppercase">{perf.artistName}</h4>
                              <span className="text-sm font-bold text-primary flex items-center gap-1.5 mt-0.5">
                                <span className="material-symbols-outlined text-sm">schedule</span>
                                {getLogicalTimeDetails(perf.startTime).timeStr} - {getLogicalTimeDetails(perf.endTime).timeStr}
                              </span>
                            </div>
                          </div>
                          
                          <div className="flex items-center justify-between md:justify-end gap-6 border-t border-white/5 pt-4 md:border-none md:pt-0">
                            {isClashing && (
                              <div className="flex items-center gap-1.5 text-coral-accent bg-coral-accent/10 px-3 py-1.5 rounded text-[10px] font-bold uppercase tracking-wider">
                                <span className="material-symbols-outlined text-xs">warning</span>
                                Overlap Detected
                              </div>
                            )}
                            <button 
                              onClick={() => handleToggleFavorite(perf.artistId)}
                              className="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 border border-white/10 flex items-center justify-center transition-all group"
                              title="Remove from My Schedule"
                            >
                              <span className="material-symbols-outlined text-tertiary group-hover:text-white transition-colors">favorite</span>
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* RENDER DETAILED SET OVERLAY POPUP */}
      {selectedSet && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center z-50 p-4 animate-fade-in" onClick={handleCloseDetails}>
          <div 
            className="w-full max-w-[460px] rounded-2xl glass-panel border border-white/20 overflow-hidden shadow-2xl zoom-in-animation flex flex-col relative"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close Button */}
            <button onClick={handleCloseDetails} className="absolute top-4 right-4 z-20 w-8 h-8 rounded-full bg-black/40 backdrop-blur-md border border-white/10 flex items-center justify-center text-white hover:bg-black/60 transition-all">
              <span className="material-symbols-outlined text-lg">close</span>
            </button>

            {/* Profile Hero image */}
            <div className="h-[210px] w-full relative bg-cover bg-center" style={{ backgroundImage: `url('${selectedSet.artistImageUrl || FALLBACK_IMAGE}')` }}>
              <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent" />
              <div className="absolute bottom-4 left-4 right-16">
                <span className="px-2.5 py-1 bg-primary text-[10px] font-black uppercase tracking-widest rounded shadow-lg">{selectedSet.stageName}</span>
                <h3 className="font-display-lg uppercase text-2xl font-bold mt-2 text-glow">{selectedSet.artistName}</h3>
              </div>
            </div>

            {/* Set details & Sync Actions */}
            <div className="p-6 flex flex-col gap-5 bg-background-light/90">
              <div className="flex items-center justify-between border-b border-white/5 pb-4">
                <div className="flex flex-col">
                  <span className="text-xs text-outline-variant font-bold uppercase tracking-wider">Set Timing</span>
                  <span className="font-headline-sm font-bold text-white text-base mt-1 flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-primary text-base">schedule</span>
                    {getLogicalTimeDetails(selectedSet.startTime).timeStr} - {getLogicalTimeDetails(selectedSet.endTime).timeStr}
                  </span>
                </div>

                <div className="flex flex-col items-end">
                  <span className="text-xs text-outline-variant font-bold uppercase tracking-wider">Status</span>
                  <span style={{
                    fontSize: '11px',
                    fontWeight: '800',
                    padding: '3px 8px',
                    borderRadius: '6px',
                    marginTop: '4px',
                    backgroundColor: selectedSet.status === 'SCHEDULED' ? 'rgba(112,0,255,0.1)' :
                                     selectedSet.status === 'LIVE' ? 'rgba(16,185,129,0.1)' :
                                     selectedSet.status === 'DELAYED' ? 'rgba(245,158,11,0.1)' : 'rgba(239,68,68,0.1)',
                    color: selectedSet.status === 'SCHEDULED' ? 'var(--primary)' :
                           selectedSet.status === 'LIVE' ? '#34D399' :
                           selectedSet.status === 'DELAYED' ? '#FBBF24' : '#F87171',
                  }}>
                    {selectedSet.status}
                  </span>
                </div>
              </div>

              {/* Loader for Details */}
              {loadingDetail ? (
                <div className="text-center py-6 text-outline-variant flex items-center justify-center gap-2 text-xs">
                  <span className="material-symbols-outlined animate-spin text-base">sync</span>
                  <span>Fetching playlists and artist biography...</span>
                </div>
              ) : (
                <>
                  {/* Biography text */}
                  {detailArtist?.biography && (
                    <div className="flex flex-col gap-1">
                      <span className="text-xs text-outline-variant font-bold uppercase tracking-wider">Biography</span>
                      <p className="text-xs text-white/75 font-medium leading-relaxed max-h-[80px] overflow-y-auto">
                        {detailArtist.biography}
                      </p>
                    </div>
                  )}

                  {/* Sync Music Preview Player */}
                  {audioPreviewUrl && detailArtist?.songs && detailArtist.songs.length > 0 && (
                    <div className="glass-panel p-4 rounded-xl border border-white/5 bg-background/40 flex items-center justify-between gap-3 shadow-[0_0_15px_rgba(255,255,255,0.02)]">
                      <div className="flex items-center gap-3 overflow-hidden">
                        <button 
                          onClick={toggleAudioPlayback}
                          className="w-11 h-10 rounded-lg bg-primary hover:bg-primary/80 flex items-center justify-center text-white transition-all shadow-md active:scale-95"
                        >
                          <span className="material-symbols-outlined text-xl">
                            {isPlayingPreview ? 'pause' : 'play_arrow'}
                          </span>
                        </button>
                        <div className="flex flex-col overflow-hidden text-left">
                          <span className="text-[10px] text-primary font-black uppercase tracking-widest">Apple Music Snippet</span>
                          <span className="text-xs font-bold text-white truncate max-w-[200px] mt-0.5">{detailArtist.songs[0].title}</span>
                        </div>
                      </div>

                      {/* Visualizer bars */}
                      {isPlayingPreview && (
                        <div className="flex items-end gap-1.5 h-6 shrink-0 pb-1.5 pr-2">
                          <div className="w-1.5 h-3 bg-primary rounded animate-pulse" />
                          <div className="w-1.5 h-4 bg-primary rounded animate-pulse [animation-delay:0.2s]" />
                          <div className="w-1.5 h-2.5 bg-primary rounded animate-pulse [animation-delay:0.4s]" />
                          <div className="w-1.5 h-5 bg-primary rounded animate-pulse [animation-delay:0.1s]" />
                        </div>
                      )}
                    </div>
                  )}

                  {/* Add Bookmark action */}
                  <div className="flex gap-3 mt-2">
                    <button
                      onClick={() => {
                        handleToggleFavorite(selectedSet.artistId);
                      }}
                      className={`flex-1 py-3 px-4 rounded-xl font-label-md uppercase tracking-wider transition-all duration-300 flex items-center justify-center gap-2 font-bold ${favoriteIds.has(selectedSet.artistId) ? 'bg-tertiary/10 border-2 border-tertiary text-tertiary shadow-[0_0_15px_rgba(0,219,233,0.2)]' : 'bg-primary text-white hover:bg-primary/80 shadow-[0_0_15px_rgba(112,0,255,0.3)]'}`}
                    >
                      <span className="material-symbols-outlined text-lg">
                        {favoriteIds.has(selectedSet.artistId) ? 'favorite' : 'favorite_border'}
                      </span>
                      <span>{favoriteIds.has(selectedSet.artistId) ? 'Bookmarked' : 'Bookmark Set'}</span>
                    </button>
                    <button
                      onClick={handleCloseDetails}
                      className="py-3 px-5 rounded-xl border border-white/10 text-xs font-bold text-outline-variant hover:text-white uppercase transition-all"
                    >
                      Close
                    </button>
                  </div>

                  {/* Add to personal schedule (My Schedule feature) */}
                  <ScheduleToggleButton
                    artistName={selectedSet.artistName}
                    stageName={selectedSet.stageName}
                    startTime={getLogicalTimeDetails(selectedSet.startTime).timeStr}
                    endTime={getLogicalTimeDetails(selectedSet.endTime).timeStr}
                    festivalDay={DAY_ID_TO_NUMBER[getLogicalTimeDetails(selectedSet.startTime).dayId] ?? 1}
                  />
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </main>
  );
};
export default Lineup;
