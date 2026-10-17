import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';

interface Artist {
  id: number;
  name: string;
  genre: string | null;
  imageUrl: string | null;
}

interface Prize {
  id: string;
  label: string;
  probability: number; // percentage
  color: string;
  textColor: string;
  isBigWin: boolean;
  codePrefix: string;
}

const PRIZES: Prize[] = [
  { id: 'ticket_4_days', label: 'Bilet 4 Zile Anul Viitor', probability: 0.1, color: 'from-[#FFD700] to-[#FFA500]', textColor: '#000000', isBigWin: true, codePrefix: 'PASS4D-2027-' },
  { id: 'ticket_1_day', label: 'Bilet 1 Zi Random', probability: 1.0, color: 'from-[#FF007F] to-[#FF5E97]', textColor: '#FFFFFF', isBigWin: true, codePrefix: 'DAYPASS-2027-' },
  { id: 'discount_10', label: '10% Reducere Bilet', probability: 20.0, color: 'from-[#00E5FF] to-[#00A3FF]', textColor: '#000000', isBigWin: false, codePrefix: 'DISC10-' },
  { id: 'discount_5', label: '5% Reducere Bilet', probability: 50.0, color: 'from-[#00FFA3] to-[#00B272]', textColor: '#000000', isBigWin: false, codePrefix: 'DISC5-' },
  { id: 'merch', label: 'Merch UNTOLD', probability: 5.0, color: 'from-[#7000FF] to-[#A04DFF]', textColor: '#FFFFFF', isBigWin: true, codePrefix: 'MERCH-' },
  { id: 'no_win', label: 'Necâștigător (Mai încearcă)', probability: 23.9, color: 'from-[#1e1e2f] to-[#2d2d42]', textColor: '#8e8e9f', isBigWin: false, codePrefix: 'TRYAGAIN-' }
];

// Wedges on the physical wheel (10 segments, beautifully distributed to interspace big prizes and greys)
interface WheelWedge {
  index: number;
  prizeId: string;
  label: string;
  color: string; // solid hex color for SVG fill
  bgClass: string; // Tailwind class
}

const WHEEL_WEDGES: WheelWedge[] = [
  { index: 0, prizeId: 'discount_5', label: 'Reducere 5%', color: '#00FFA3', bgClass: 'from-[#00FFA3] to-[#00B272]' },
  { index: 1, prizeId: 'no_win', label: 'Mai încearcă 😢', color: '#1e1e2f', bgClass: 'from-[#1e1e2f] to-[#2d2d42]' },
  { index: 2, prizeId: 'discount_10', label: 'Reducere 10%', color: '#00E5FF', bgClass: 'from-[#00E5FF] to-[#00A3FF]' },
  { index: 3, prizeId: 'ticket_1_day', label: 'Bilet 1 Zi 🎫', color: '#FF007F', bgClass: 'from-[#FF007F] to-[#FF5E97]' },
  { index: 4, prizeId: 'discount_5', label: 'Reducere 5%', color: '#00FFA3', bgClass: 'from-[#00FFA3] to-[#00B272]' },
  { index: 5, prizeId: 'no_win', label: 'Mai încearcă 😢', color: '#1e1e2f', bgClass: 'from-[#1e1e2f] to-[#2d2d42]' },
  { index: 6, prizeId: 'merch', label: 'Merch UNTOLD 👕', color: '#7000FF', bgClass: 'from-[#7000FF] to-[#A04DFF]' },
  { index: 7, prizeId: 'discount_5', label: 'Reducere 5%', color: '#00FFA3', bgClass: 'from-[#00FFA3] to-[#00B272]' },
  { index: 8, prizeId: 'ticket_4_days', label: 'Abonament 4Z ⭐', color: '#FFD700', bgClass: 'from-[#FFD700] to-[#FFA500]' },
  { index: 9, prizeId: 'no_win', label: 'Mai încearcă 😢', color: '#1e1e2f', bgClass: 'from-[#1e1e2f] to-[#2d2d42]' }
];

interface FestivalReviewWheelProps {
  isEligible: boolean;
  tickets: any[];
}

export const FestivalReviewWheel: React.FC<FestivalReviewWheelProps> = ({ isEligible, tickets }) => {
  const { user } = useAuth();
  const { language } = useI18n();
  
  // State for Review Form
  const [artists, setArtists] = useState<Artist[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedArtists, setSelectedArtists] = useState<Artist[]>([]);
  const [feedback, setFeedback] = useState('');
  const [suggestions, setSuggestions] = useState('');
  const [isLoadingArtists, setIsLoadingArtists] = useState(false);
  const [errorText, setErrorText] = useState<string | null>(null);

  // Flow State
  // 'review' -> 'wheel' -> 'result'
  const [flowStage, setFlowStage] = useState<'review' | 'wheel' | 'result'>('review');
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);
  const [hasSubmittedReview, setHasSubmittedReview] = useState(false);

  // Wheel Spin State
  const [isSpinning, setIsSpinning] = useState(false);
  const [spinRotation, setSpinRotation] = useState(0);
  const [wonPrize, setWonPrize] = useState<Prize | null>(null);
  const [uniqueVoucherCode, setUniqueVoucherCode] = useState<string>('');
  
  // Audio Feedback (Synth oscillator for high-premium clicking effect during spin)
  const audioContextRef = useRef<AudioContext | null>(null);

  // Mock fallback artists in case the backend API is empty/offline
  const FALLBACK_ARTISTS: Artist[] = [
    { id: 101, name: 'Armin van Buuren', genre: 'Trance / EDM', imageUrl: null },
    { id: 102, name: 'Martin Garrix', genre: 'EDM / Progressive House', imageUrl: null },
    { id: 103, name: 'David Guetta', genre: 'EDM / Dance', imageUrl: null },
    { id: 104, name: 'Steve Aoki', genre: 'EDM / Electro House', imageUrl: null },
    { id: 105, name: 'Tale Of Us', genre: 'Melodic Techno', imageUrl: null },
    { id: 106, name: 'Boris Brejcha', genre: 'High-Tech Minimal', imageUrl: null },
    { id: 107, name: 'Peggy Gou', genre: 'House / Techno', imageUrl: null },
    { id: 108, name: 'Hardwell', genre: 'Big Room / Techno', imageUrl: null },
    { id: 109, name: 'Alok', genre: 'Deep House / Dance', imageUrl: null },
    { id: 110, name: 'Lost Frequencies', genre: 'Deep House', imageUrl: null },
    { id: 111, name: 'Charlotte de Witte', genre: 'K-Techno', imageUrl: null },
    { id: 112, name: 'Sam Smith', genre: 'Pop / Live Act', imageUrl: null }
  ];

  // Load existing state from localStorage on mount
  useEffect(() => {
    if (!user) return;
    
    const submitted = localStorage.getItem(`untold_review_submitted_${user.id}`);
    const prizeIdSaved = localStorage.getItem(`untold_spin_prize_${user.id}`);
    const voucherSaved = localStorage.getItem(`untold_spin_voucher_${user.id}`);
    
    if (submitted === 'true') {
      setHasSubmittedReview(true);
      if (prizeIdSaved) {
        const found = PRIZES.find(p => p.id === prizeIdSaved);
        if (found) {
          setWonPrize(found);
          setUniqueVoucherCode(voucherSaved || '');
          setFlowStage('result');
        } else {
          setFlowStage('wheel');
        }
      } else {
        setFlowStage('wheel');
      }
    }

    // Fetch artists from API
    const fetchArtists = async () => {
      setIsLoadingArtists(true);
      try {
        const res = await fetch('http://localhost:8080/api/artists');
        if (res.ok) {
          const data = await res.json();
          if (data && data.data && data.data.length > 0) {
            setArtists(data.data);
          } else {
            setArtists(FALLBACK_ARTISTS);
          }
        } else {
          setArtists(FALLBACK_ARTISTS);
        }
      } catch (err) {
        console.error('Failed to load artists from API, using fallback list', err);
        setArtists(FALLBACK_ARTISTS);
      } finally {
        setIsLoadingArtists(false);
      }
    };

    fetchArtists();
  }, [user]);

  // Audio Ticking Generator
  const playTickSound = () => {
    try {
      if (!audioContextRef.current) {
        audioContextRef.current = new (window.AudioContext || (window as any).webkitAudioContext)();
      }
      const ctx = audioContextRef.current;
      if (ctx.state === 'suspended') {
        ctx.resume();
      }
      
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(400, ctx.currentTime); // High-pitched click
      osc.frequency.exponentialRampToValueAtTime(100, ctx.currentTime + 0.05);
      
      gain.gain.setValueAtTime(0.08, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.05);
      
      osc.connect(gain);
      gain.connect(ctx.destination);
      
      osc.start();
      osc.stop(ctx.currentTime + 0.05);
    } catch (e) {
      // Audio context might be blocked or not supported, ignore silently
    }
  };

  // Filtered list of artists for searching
  const filteredArtists = artists.filter(artist => 
    artist.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    (artist.genre && artist.genre.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const handleSelectArtist = (artist: Artist) => {
    if (selectedArtists.some(a => a.id === artist.id)) {
      setSelectedArtists(prev => prev.filter(a => a.id !== artist.id));
    } else {
      if (selectedArtists.length >= 3) {
        // Replace the last one or do nothing
        return;
      }
      setSelectedArtists(prev => [...prev, artist]);
    }
  };

  const handleRemoveArtist = (artistId: number) => {
    setSelectedArtists(prev => prev.filter(a => a.id !== artistId));
  };

  const handleSubmitReview = (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    
    if (selectedArtists.length !== 3) {
      setErrorText(language === 'ro' ? 'Te rugăm să selectezi exact 3 artiști preferați.' : 'Please select exactly 3 favorite artists.');
      return;
    }
    if (!feedback.trim()) {
      setErrorText(language === 'ro' ? 'Te rugăm să introduci părerea ta despre festival.' : 'Please write your feedback about the festival.');
      return;
    }
    if (!suggestions.trim()) {
      setErrorText(language === 'ro' ? 'Te rugăm să introduci câteva sugestii.' : 'Please provide some suggestions.');
      return;
    }

    setIsSubmittingReview(true);
    setErrorText(null);

    // Simulate sending data to API
    setTimeout(() => {
      setIsSubmittingReview(false);
      setHasSubmittedReview(true);
      localStorage.setItem(`untold_review_submitted_${user.id}`, 'true');
      setFlowStage('wheel');
    }, 1500);
  };

  // Wheel Spin Trigger
  const handleSpinWheel = () => {
    if (isSpinning || !user) return;

    setIsSpinning(true);
    
    // Choose winner based on exact probability
    const rand = Math.random() * 1000; // 0 to 1000 (representing 0% to 100.0%)
    let selectedPrize: Prize;

    if (rand < 1) { // 0.1% chance
      selectedPrize = PRIZES[0]; // Bilet 4 Zile
    } else if (rand < 11) { // 1.0% chance (1 + 10 = 11)
      selectedPrize = PRIZES[1]; // Bilet 1 Zi
    } else if (rand < 61) { // 5.0% chance (11 + 50 = 61)
      selectedPrize = PRIZES[4]; // Merch UNTOLD
    } else if (rand < 261) { // 20.0% chance (61 + 200 = 261)
      selectedPrize = PRIZES[2]; // 10% Reducere
    } else if (rand < 761) { // 50.0% chance (261 + 500 = 761)
      selectedPrize = PRIZES[3]; // 5% Reducere
    } else { // 23.9% chance
      selectedPrize = PRIZES[5]; // Necastigator
    }

    // Map won prize to one of the visual wedges
    const eligibleWedges = WHEEL_WEDGES.filter(w => w.prizeId === selectedPrize.id);
    // Pick one wedge at random if there are multiple (e.g. 5% discount appears on 3 wedges)
    const chosenWedge = eligibleWedges[Math.floor(Math.random() * eligibleWedges.length)];
    const wedgeIndex = chosenWedge.index;

    // Spin calculation
    // A full circle is 360 deg. 10 wedges means each wedge is 36 deg.
    // Wedge centers are at: index * 36 + 18 deg.
    // The pointer points at 90 degrees (right) or 270 degrees (top).
    // Let's assume the indicator is at 270 degrees (top).
    // To make the wheel stop exactly at the center of the chosen wedge under the top pointer:
    // rotationOffset = 270 - (wedgeIndex * 36 + 18)
    const wedgeCenter = (wedgeIndex * 36) + 18;
    const targetWedgeAngle = (360 - wedgeCenter + 270) % 360; // Align with top pointer at 270 deg
    
    // Add 8 full spins for an intense, gorgeous animation sequence (8 * 360 = 2880)
    const extraSpins = 8 * 360;
    const totalRotation = extraSpins + targetWedgeAngle;
    
    setSpinRotation(totalRotation);

    // Audio click emulation during spin
    let currentRotation = 0;
    let clickStep = 36; // Click every 36 degrees (at wedge boundaries)
    let lastClickAngle = 0;

    const interval = setInterval(() => {
      // This is a rough estimation of rotation progress to play tick sounds
      // The CSS transition uses a cubic-bezier ease-out, meaning it slows down near the end
      // In React we can't easily get the real-time CSS transform, so we just run a ticking interval that slows down
    }, 50);

    // Play ticking audio on a curve
    const duration = 5000; // 5 seconds spin animation
    const startTime = Date.now();
    
    const playTicks = () => {
      const elapsed = Date.now() - startTime;
      if (elapsed >= duration) return;

      playTickSound();
      
      // Decelerating ticks
      const progress = elapsed / duration;
      const delay = 50 + progress * progress * 800; // Delay gets longer exponentially
      setTimeout(playTicks, delay);
    };
    
    setTimeout(playTicks, 100);

    // Complete the spin
    setTimeout(() => {
      setIsSpinning(false);
      clearInterval(interval);
      setWonPrize(selectedPrize);
      
      // Generate a beautiful unique mock code
      const randHex = Math.random().toString(16).substring(2, 8).toUpperCase();
      const code = selectedPrize.id === 'no_win' 
        ? '' 
        : `${selectedPrize.codePrefix}${randHex}`;
      
      setUniqueVoucherCode(code);

      // Save states
      localStorage.setItem(`untold_spin_prize_${user.id}`, selectedPrize.id);
      localStorage.setItem(`untold_spin_voucher_${user.id}`, code);
      setFlowStage('result');
    }, duration);
  };

  // Get ticket type information
  const getTicketEligibilityReason = () => {
    if (tickets.length === 0) {
      return language === 'ro' 
        ? 'Nu ai niciun bilet asociat contului tău în acest moment.' 
        : 'You do not have any tickets associated with your account at this moment.';
    }
    const hasOneDayOnly = tickets.every(t => t.ticketType.includes('DAY'));
    if (hasOneDayOnly) {
      return language === 'ro'
        ? 'Deții doar bilete de o zi. Roata norocului este exclusivă pentru posesorii de abonamente (4-Day Pass / VIP).'
        : 'You only hold 1-Day tickets. The Wheel of Fortune is exclusive for 4-Day Pass or VIP holders.';
    }
    const hasNoCheckIn = tickets.every(t => !t.checkInCompleted);
    if (hasNoCheckIn) {
      return language === 'ro'
        ? 'Trebuie să finalizezi online Check-In-ul pentru abonamentul tău în tab-ul "Biletele Mele" pentru a debloca această secțiune.'
        : 'You must complete online Check-In for your multi-day pass in the "My Tickets" tab to unlock this section.';
    }
    return language === 'ro'
      ? 'Abonamentul tău nu corespunde criteriilor de eligibilitate.'
      : 'Your pass does not meet the eligibility criteria.';
  };

  // RENDER BLOCKED STATE (Non-eligible users)
  if (!isEligible) {
    return (
      <div className="glass-panel p-8 md:p-12 rounded-3xl border border-red-500/10 max-w-3xl mx-auto text-center relative overflow-hidden bg-gradient-to-b from-[#0f0714] to-[#04020a]">
        <div className="absolute inset-0 bg-red-500/[0.01] pointer-events-none" />
        
        {/* Glowing aura */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 rounded-full bg-red-500/5 filter blur-[60px]" />

        <div className="relative z-10">
          <div className="w-20 h-24 rounded-2xl bg-red-500/10 flex items-center justify-center mb-6 mx-auto border border-red-500/20 shadow-[0_0_15px_rgba(239,68,68,0.15)] animate-pulse">
            <span className="material-symbols-outlined text-4xl text-red-500">lock</span>
          </div>

          <h3 className="text-xl md:text-2xl font-black text-white uppercase tracking-tight mb-3">
            {language === 'ro' ? 'Acces Restricționat' : 'Access Restricted'}
          </h3>
          
          <p className="text-sm text-white/70 max-w-md mx-auto leading-relaxed mb-6">
            {getTicketEligibilityReason()}
          </p>

          <div className="bg-white/[0.02] border border-white/5 rounded-2xl p-4 text-xs text-white/50 text-left max-w-md mx-auto space-y-2 leading-relaxed">
            <p className="font-bold text-white text-xs uppercase tracking-wide flex items-center gap-1.5 text-primary">
              <span className="material-symbols-outlined text-xs">info</span>
              {language === 'ro' ? 'Cum pot debloca roata?' : 'How can I unlock the wheel?'}
            </p>
            <ul className="list-disc pl-4 space-y-1">
              <li>{language === 'ro' ? 'Asigură-te că ești conectat în aplicație.' : 'Ensure you are logged in to the app.'}</li>
              <li>{language === 'ro' ? 'Trebuie să ai un bilet de tip Abonament 4 Zile (GA sau VIP).' : 'You must hold a 4-Day Pass ticket (GA or VIP).'}</li>
              <li>{language === 'ro' ? 'Finalizează procesul de Check-In pentru acel bilet.' : 'Complete the online Check-In process for that ticket.'}</li>
            </ul>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      
      {/* HEADER HERO */}
      <div className="text-center mb-10 relative">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-32 rounded-full bg-primary/10 filter blur-[80px] pointer-events-none" />
        <span className="bg-primary/20 border border-primary/30 text-primary-fixed-dim text-[10px] uppercase font-bold tracking-widest px-4 py-1 rounded-full inline-block mb-3.5 shadow-[0_0_15px_rgba(112,0,255,0.15)]">
          ✨ {language === 'ro' ? 'Experiența Untold 2026' : 'Untold 2026 Experience'}
        </span>
        <h2 className="text-3xl md:text-4xl font-black text-white uppercase tracking-tight">
          {language === 'ro' ? 'Review Festival & Roata Norocului' : 'Festival Review & Wheel of Fortune'}
        </h2>
        <p className="text-xs md:text-sm text-white/60 max-w-lg mx-auto leading-relaxed mt-2">
          {language === 'ro' 
            ? 'Spune-ne ce artiști ți-au plăcut cel mai mult și oferă părerea ta sinceră pentru a debloca o rotire garantată la marea Roată a Premiilor UNTOLD!'
            : 'Tell us which artists you loved most and provide your honest feedback to unlock a guaranteed spin on the grand UNTOLD Prize Wheel!'}
        </p>
      </div>

      {/* STAGE 1: THE REVIEW FORM */}
      {flowStage === 'review' && (
        <form onSubmit={handleSubmitReview} className="glass-panel p-6 md:p-10 rounded-3xl border border-white/10 space-y-8 bg-gradient-to-b from-[#090916] to-[#040409] relative shadow-[0_15px_40px_rgba(112,0,255,0.1)]">
          <div className="absolute inset-0 bg-primary/[0.01] pointer-events-none rounded-3xl" />
          
          {/* Section 1: Artist Selection */}
          <div className="space-y-4">
            <div className="border-b border-white/5 pb-2">
              <h3 className="text-lg font-black text-white uppercase tracking-tight flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">music_note</span>
                1. {language === 'ro' ? 'Top 3 Artiști Preferați' : '1. Top 3 Favorite Artists'}
                <span className="text-[10px] font-medium text-white/40 normal-case ml-2">
                  ({selectedArtists.length} din 3 selectați)
                </span>
              </h3>
              <p className="text-xs text-white/50">
                {language === 'ro' ? 'Alege exact 3 artiști care te-au impresionat cel mai mult în această ediție.' : 'Choose exactly 3 artists who impressed you the most in this edition.'}
              </p>
            </div>

            {/* Selected Artists Tags Container */}
            {selectedArtists.length > 0 && (
              <div className="flex flex-wrap gap-2.5 bg-white/[0.02] border border-white/5 p-3 rounded-2xl">
                {selectedArtists.map(artist => (
                  <div key={artist.id} className="flex items-center gap-2 bg-primary/20 border border-primary/30 rounded-xl pl-3.5 pr-2 py-1.5">
                    <span className="text-xs font-bold text-white tracking-wide">{artist.name}</span>
                    <button 
                      type="button" 
                      onClick={() => handleRemoveArtist(artist.id)}
                      className="w-5 h-5 rounded-lg bg-black/30 flex items-center justify-center text-white/60 hover:text-white hover:bg-black/50 transition-colors"
                    >
                      <span className="material-symbols-outlined text-sm">close</span>
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Artist Search & Dropdown List */}
            {selectedArtists.length < 3 && (
              <div className="space-y-3">
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 material-symbols-outlined text-white/40 text-lg">search</span>
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder={language === 'ro' ? 'Caută artiști sau genuri muzicale...' : 'Search artists or genres...'}
                    className="w-full bg-white/5 border border-white/10 rounded-xl py-3 pl-11 pr-4 text-white text-xs font-semibold focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary transition-all placeholder:text-white/30"
                  />
                  {searchQuery && (
                    <button
                      type="button"
                      onClick={() => setSearchQuery('')}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 material-symbols-outlined text-white/40 text-lg hover:text-white"
                    >
                      close
                    </button>
                  )}
                </div>

                {/* Grid of options */}
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2.5 max-h-56 overflow-y-auto pr-1.5 custom-scrollbar">
                  {isLoadingArtists ? (
                    <div className="col-span-full py-6 text-center text-xs text-white/40 animate-pulse">
                      {language === 'ro' ? 'Se încarcă artiștii...' : 'Loading artists...'}
                    </div>
                  ) : filteredArtists.length === 0 ? (
                    <div className="col-span-full py-6 text-center text-xs text-white/40">
                      {language === 'ro' ? 'Nu s-au găsit artiști.' : 'No artists found.'}
                    </div>
                  ) : (
                    filteredArtists.map(artist => {
                      const isSelected = selectedArtists.some(a => a.id === artist.id);
                      return (
                        <button
                          key={artist.id}
                          type="button"
                          onClick={() => handleSelectArtist(artist)}
                          className={`group text-left p-3 rounded-xl border transition-all flex flex-col justify-between h-20 ${isSelected ? 'bg-primary/20 border-primary text-white shadow-[0_0_15px_rgba(112,0,255,0.15)]' : 'bg-white/[0.02] border-white/5 hover:border-white/10 hover:bg-white/[0.04] text-white/70'}`}
                        >
                          <p className="text-xs font-bold leading-tight line-clamp-2">{artist.name}</p>
                          <span className={`text-[9px] uppercase tracking-widest font-bold ${isSelected ? 'text-primary-fixed-dim' : 'text-white/40'}`}>
                            {artist.genre || 'Live Set'}
                          </span>
                        </button>
                      );
                    })
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Section 2: Opinion Text Area */}
          <div className="space-y-3.5">
            <div className="border-b border-white/5 pb-2">
              <h3 className="text-lg font-black text-white uppercase tracking-tight flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">reviews</span>
                2. {language === 'ro' ? 'Părerea ta sinceră' : '2. Your Honest Opinion'}
              </h3>
              <p className="text-xs text-white/50">
                {language === 'ro' ? 'Ce ți-a plăcut cel mai mult în această ediție? Spune-ne impresiile tale.' : 'What did you love most in this edition? Share your thoughts.'}
              </p>
            </div>
            <textarea
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
              rows={4}
              placeholder={language === 'ro' ? 'Atmosfera de la Mainstage a fost magică, organizarea accesului foarte rapidă...' : 'The Mainstage atmosphere was magical, gate access was smooth...'}
              className="w-full bg-white/5 border border-white/10 rounded-2xl p-4 text-white text-xs font-medium focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary transition-all placeholder:text-white/20 leading-relaxed"
            />
          </div>

          {/* Section 3: Suggestions */}
          <div className="space-y-3.5">
            <div className="border-b border-white/5 pb-2">
              <h3 className="text-lg font-black text-white uppercase tracking-tight flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">help</span>
                3. {language === 'ro' ? 'Sugestii de îmbunătățire' : '3. Suggestions for Improvement'}
              </h3>
              <p className="text-xs text-white/50">
                {language === 'ro' ? 'Ce crezi că am putea optimiza pentru anul viitor?' : 'What do you think we can optimize for next year?'}
              </p>
            </div>
            <textarea
              value={suggestions}
              onChange={(e) => setSuggestions(e.target.value)}
              rows={3}
              placeholder={language === 'ro' ? 'Mai multe puncte de apă gratuită în festival, extinderea zonelor de mâncare...' : 'More free water refill stations, larger food court areas...'}
              className="w-full bg-white/5 border border-white/10 rounded-2xl p-4 text-white text-xs font-medium focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary transition-all placeholder:text-white/20 leading-relaxed"
            />
          </div>

          {errorText && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3.5 text-xs font-bold text-red-400 flex items-center gap-2">
              <span className="material-symbols-outlined text-base">warning</span>
              {errorText}
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isSubmittingReview || selectedArtists.length !== 3}
            className="w-full bg-gradient-to-r from-primary to-tertiary hover:from-primary hover:to-tertiary-container text-on-primary font-black uppercase tracking-wider text-xs py-4 rounded-xl transition-all shadow-[0_0_20px_rgba(112,0,255,0.3)] hover:shadow-[0_0_35px_rgba(112,0,255,0.6)] disabled:opacity-35 disabled:cursor-not-allowed disabled:shadow-none hover:-translate-y-0.5 transform active:scale-95 duration-300"
          >
            {isSubmittingReview ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                {language === 'ro' ? 'Se salvează review-ul...' : 'Saving review...'}
              </span>
            ) : (
              language === 'ro' ? 'Trimite Review-ul și deblochează Roata!' : 'Submit Review & Unlock Wheel!'
            )}
          </button>
        </form>
      )}

      {/* STAGE 2: THE WHEEL OF FORTUNE */}
      {flowStage === 'wheel' && (
        <div className="glass-panel p-6 md:p-10 rounded-3xl border border-white/10 flex flex-col items-center justify-center bg-gradient-to-b from-[#090916] to-[#040409] relative shadow-[0_15px_40px_rgba(112,0,255,0.1)] overflow-hidden">
          
          {/* Subtle neon beams */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] rounded-full bg-tertiary/5 filter blur-[100px] pointer-events-none" />

          {/* SUCCESS BANNER */}
          <div className="bg-green-500/10 border border-green-500/20 text-green-400 rounded-2xl px-5 py-3 text-center mb-8 flex items-center gap-2 text-xs font-black uppercase tracking-wider shadow-[0_0_15px_rgba(34,197,94,0.1)]">
            <span className="material-symbols-outlined text-green-500 animate-bounce">check_circle</span>
            {language === 'ro' ? 'Review salvat! Roata s-a deblocat.' : 'Review Saved! Wheel Unlocked.'}
          </div>

          {/* THE PHYSICAL WHEEL CONTAINER */}
          <div className="relative w-72 h-72 sm:w-80 sm:h-80 md:w-96 md:h-96 flex items-center justify-center mb-8 shrink-0">
            
            {/* BACKGROUND GLOW */}
            <div className="absolute inset-0 rounded-full border border-tertiary/20 shadow-[0_0_50px_rgba(0,219,233,0.15)] bg-black/40" />

            {/* TOP POINTER */}
            <div className="absolute -top-3 z-30 flex flex-col items-center">
              {/* Triangular glowing pointer pointing down */}
              <div className="w-0 h-0 border-l-[14px] border-l-transparent border-r-[14px] border-r-transparent border-t-[20px] border-t-tertiary drop-shadow-[0_0_8px_rgba(0,219,233,0.8)] filter" />
              <div className="w-3 h-3 rounded-full bg-white -mt-5 shadow-[0_0_8px_white]" />
            </div>

            {/* ROTATING ELEMENT */}
            <div 
              style={{
                transform: `rotate(${spinRotation}deg)`,
                transition: isSpinning ? 'transform 5000ms cubic-bezier(0.12, 0.8, 0.15, 1)' : 'none'
              }}
              className="absolute w-full h-full rounded-full transition-transform duration-1000 ease-out origin-center select-none"
            >
              {/* SVG WHEEL GRAPHIC */}
              <svg 
                viewBox="0 0 100 100" 
                className="w-full h-full drop-shadow-[0_0_15px_rgba(112,0,255,0.2)]"
              >
                {/* Defs for gradients */}
                <defs>
                  {WHEEL_WEDGES.map(wedge => (
                    <radialGradient key={`grad-${wedge.index}`} id={`grad-${wedge.index}`} cx="50%" cy="50%" r="50%">
                      <stop offset="0%" stopColor="#08081a" />
                      <stop offset="70%" stopColor="#0a0a22" />
                      <stop offset="100%" stopColor={wedge.color} stopOpacity="0.4" />
                    </radialGradient>
                  ))}
                  <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
                    <feGaussianBlur stdDeviation="2" result="blur" />
                    <feComposite in="SourceGraphic" in2="blur" operator="over" />
                  </filter>
                </defs>

                {/* Render the 10 wedges */}
                {WHEEL_WEDGES.map((wedge) => {
                  const startAngle = wedge.index * 36 - 90; // Start offset to rotate properly
                  const endAngle = (wedge.index + 1) * 36 - 90;
                  
                  // Convert polar to cartesian
                  const x1 = 50 + 50 * Math.cos((startAngle * Math.PI) / 180);
                  const y1 = 50 + 50 * Math.sin((startAngle * Math.PI) / 180);
                  const x2 = 50 + 50 * Math.cos((endAngle * Math.PI) / 180);
                  const y2 = 50 + 50 * Math.sin((endAngle * Math.PI) / 180);

                  const pathData = `M 50 50 L ${x1} ${y1} A 50 50 0 0 1 ${x2} ${y2} Z`;

                  // Calculate text angle and position (center of wedge)
                  const textAngle = startAngle + 18;
                  const textRad = (textAngle * Math.PI) / 180;
                  const textX = 50 + 32 * Math.cos(textRad);
                  const textY = 50 + 32 * Math.sin(textRad);

                  return (
                    <g key={wedge.index} className="cursor-default">
                      <path 
                        d={pathData} 
                        fill={`url(#grad-${wedge.index})`}
                        stroke="#1c1c38"
                        strokeWidth="0.5"
                      />
                      {/* Colored border highlight for the wedge arc */}
                      <path 
                        d={`M ${x1} ${y1} A 50 50 0 0 1 ${x2} ${y2}`}
                        fill="none"
                        stroke={wedge.color}
                        strokeWidth="1.5"
                        opacity="0.8"
                      />
                      {/* Text label rotated outward */}
                      <text
                        x={textX}
                        y={textY}
                        fill={wedge.prizeId === 'ticket_4_days' ? '#FFD700' : '#FFFFFF'}
                        fontSize="2.4"
                        fontWeight="black"
                        fontFamily="monospace"
                        textAnchor="middle"
                        alignmentBaseline="middle"
                        transform={`rotate(${textAngle + 90}, ${textX}, ${textY})`}
                        opacity={wedge.prizeId === 'no_win' ? '0.4' : '1'}
                      >
                        {wedge.label}
                      </text>
                    </g>
                  );
                })}

                {/* INNER DECORATIVE RINGS */}
                <circle cx="50" cy="50" r="12" fill="#040409" stroke="#1c1c38" strokeWidth="1" />
                <circle cx="50" cy="50" r="10" fill="#08081a" stroke="url(#grad-8)" strokeWidth="0.5" />
              </svg>

              {/* SHINY LOGO IN THE CENTER */}
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-20 h-20 rounded-full flex flex-col items-center justify-center pointer-events-none bg-[#03030d] border border-white/10 shadow-[inset_0_0_10px_rgba(255,255,255,0.1)]">
                <span className="material-symbols-outlined text-xl text-tertiary animate-pulse drop-shadow-[0_0_10px_rgba(0,219,233,0.5)]">casino</span>
                <span className="text-[7px] font-black uppercase tracking-widest text-white/50 mt-1 font-mono">UNTOLD</span>
              </div>
            </div>

            {/* BUTTON TO SPIN */}
            <button
              onClick={handleSpinWheel}
              disabled={isSpinning}
              className="absolute z-20 w-16 h-16 rounded-full bg-gradient-to-r from-tertiary to-primary hover:scale-105 active:scale-95 transition-all text-white font-black uppercase tracking-wider text-[10px] flex items-center justify-center text-center shadow-[0_0_25px_rgba(0,219,233,0.4)] hover:shadow-[0_0_35px_rgba(0,219,233,0.8)] disabled:opacity-40 disabled:scale-100 disabled:shadow-none font-mono"
            >
              {isSpinning ? 'SPIN...' : 'SPIN'}
            </button>
          </div>

          {/* SPINNERS SUBTEXT */}
          <div className="text-center max-w-sm mt-4">
            <p className="text-xs text-white/60 leading-relaxed font-semibold">
              {isSpinning 
                ? (language === 'ro' ? 'Se învârte... norocul se decide acum! 🌌' : 'Spinning... fortune is being decided! 🌌')
                : (language === 'ro' ? 'Apasă butonul din mijloc pentru a-ți asigura premiul!' : 'Press the middle button to claim your prize!')}
            </p>
            {!isSpinning && (
              <p className="text-[10px] font-mono text-tertiary font-bold tracking-widest uppercase mt-2">
                {language === 'ro' ? 'GARANTAT 1 SPIN PER PARTICIPANT' : 'GUARANTEED 1 SPIN PER ATTENDEE'}
              </p>
            )}
          </div>
        </div>
      )}

      {/* STAGE 3: THE RESULT / CLAIM VOUCHER */}
      {flowStage === 'result' && wonPrize && (
        <div className="glass-panel p-6 md:p-12 rounded-3xl border border-tertiary/20 flex flex-col items-center justify-center bg-gradient-to-b from-[#0a0a22] to-[#040409] relative shadow-[0_20px_50px_rgba(0,219,233,0.15)] text-center overflow-hidden">
          
          {/* Glowing neon burst */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 rounded-full bg-tertiary/10 filter blur-[80px] pointer-events-none animate-pulse" />
          
          {/* Confetti celebration for winners */}
          {wonPrize.id !== 'no_win' && (
            <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(0,219,233,0.05),transparent)] pointer-events-none animate-pulse" />
          )}

          <div className="relative z-10 space-y-6 max-w-md mx-auto">
            {/* Reward Icon */}
            <div className={`w-24 h-24 rounded-full flex items-center justify-center mx-auto border shadow-lg ${wonPrize.isBigWin ? 'bg-[#FFD700]/10 border-[#FFD700]/30 text-[#FFD700]' : 'bg-tertiary/10 border-tertiary/30 text-tertiary'}`}>
              <span className="material-symbols-outlined text-5xl animate-bounce">
                {wonPrize.id === 'no_win' ? 'sentiment_dissatisfied' : 'emoji_events'}
              </span>
            </div>

            {/* Celebration Title */}
            <div>
              <p className="text-[10px] font-black uppercase tracking-widest text-white/50 font-mono">
                {wonPrize.id === 'no_win' ? 'Mulțumim pentru participare!' : 'EXTREME WINNER!'}
              </p>
              <h3 className={`text-2xl md:text-3xl font-black uppercase tracking-tight mt-1 ${wonPrize.isBigWin ? 'text-[#FFD700]' : 'text-white'}`}>
                {wonPrize.id === 'no_win' 
                  ? (language === 'ro' ? 'Mai încearcă anul viitor!' : 'Better luck next time!')
                  : wonPrize.label}
              </h3>
            </div>

            {/* Prize Description / Claim Details */}
            <div className="text-xs md:text-sm text-white/70 leading-relaxed max-w-sm mx-auto">
              {wonPrize.id === 'no_win' ? (
                <p>
                  {language === 'ro' 
                    ? 'Din păcate de data aceasta nu ai fost extras ca și câștigător. Îți mulțumim extrem de mult pentru review-ul tău! Ne ajută enorm să facem UNTOLD mai magic!'
                    : 'Unfortunately you were not drawn as a winner this time. Thank you so much for your feedback! It helps us make UNTOLD even more magical!'}
                </p>
              ) : (
                <div className="space-y-4">
                  <p>
                    {language === 'ro'
                      ? 'Felicitări! Premiul tău a fost securizat. Folosește codul unic de mai jos pentru a-ți revendica premiul pe e-mail sau la magazinele partenere.'
                      : 'Congratulations! Your prize is secured. Use the unique code below to redeem your prize via email or partner stores.'}
                  </p>
                  
                  {/* Voucher code box */}
                  <div className="p-4 bg-black/40 border border-white/5 rounded-2xl relative shadow-inner">
                    <p className="text-[9px] font-bold text-white/40 uppercase tracking-widest mb-1.5 font-mono">Voucher Code Securizat</p>
                    <span className="font-mono text-base md:text-lg font-black tracking-widest bg-white/5 border border-white/10 px-4 py-2 rounded-lg text-tertiary inline-block drop-shadow-[0_0_15px_rgba(0,219,233,0.5)]">
                      {uniqueVoucherCode}
                    </span>
                    <button
                      onClick={() => {
                        navigator.clipboard.writeText(uniqueVoucherCode);
                        alert(language === 'ro' ? 'Cod copiat în clipboard!' : 'Code copied to clipboard!');
                      }}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-white/40 hover:text-white transition-colors"
                      title="Copiază Codul"
                    >
                      <span className="material-symbols-outlined text-sm">content_copy</span>
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Back button or notice */}
            <div className="pt-4">
              <p className="text-[9px] text-white/30 font-mono uppercase tracking-widest leading-relaxed">
                {language === 'ro' ? 'ID Participant' : 'Attendee ID'}: {user?.id}
                <br />
                {language === 'ro' ? 'Sursă: Asymmetric Validation Node' : 'Source: Asymmetric Validation Node'}
              </p>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
