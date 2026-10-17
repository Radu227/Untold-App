import { useState, useEffect } from 'react';
import { 
  Shield, 
  HeartPulse, 
  Flame, 
  MapPin, 
  CheckCircle, 
  RefreshCw, 
  AlertTriangle,
  Clock
} from 'lucide-react';

interface EmergencyAlert {
  id: number;
  serviceType: string; // 'POLICE', 'AMBULANCE', 'FIREFIGHTERS'
  pinId: number;
  pinName: string;
  status: string; // 'PENDING', 'RESOLVED'
  createdAt: string;
}

export const Alerts = () => {
  const [alerts, setAlerts] = useState<EmergencyAlert[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'ALL' | 'PENDING' | 'RESOLVED'>('ALL');
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  const fetchAlerts = async (quiet = false) => {
    if (!quiet) setLoading(true);
    else setIsRefreshing(true);
    
    try {
      const response = await fetch('http://localhost:8080/api/alerts');
      if (!response.ok) {
        throw new Error(`API error ${response.status}`);
      }
      const data = await response.json();
      if (data && data.type === 'SUCCESS') {
        setAlerts(data.data || []);
        setError(null);
      } else {
        throw new Error(data?.message || 'Eroare la preluarea alertelor');
      }
    } catch (err: any) {
      console.error(err);
      setError('Nu s-a putut face conexiunea cu serverul de backend. Folosim alerte simulate ca fallback.');
      // Fallback alerts for testing UI styling offline
      if (alerts.length === 0) {
        setAlerts([
          {
            id: 1,
            serviceType: 'POLICE',
            pinId: 101,
            pinName: 'Mainstage Arena',
            status: 'PENDING',
            createdAt: new Date(Date.now() - 5 * 60000).toISOString()
          },
          {
            id: 2,
            serviceType: 'AMBULANCE',
            pinId: 105,
            pinName: 'Cocktail Bar East',
            status: 'PENDING',
            createdAt: new Date(Date.now() - 15 * 60000).toISOString()
          },
          {
            id: 3,
            serviceType: 'FIREFIGHTERS',
            pinId: 104,
            pinName: 'Food Court North',
            status: 'RESOLVED',
            createdAt: new Date(Date.now() - 120 * 60000).toISOString()
          }
        ]);
      }
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
    // Auto refresh alerts every 10 seconds for real-time monitoring!
    const timer = setInterval(() => {
      fetchAlerts(true);
    }, 10000);
    return () => clearInterval(timer);
  }, []);

  const handleResolveAlert = async (id: number) => {
    try {
      const response = await fetch(`http://localhost:8080/api/alerts/${id}/resolve`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      const data = await response.json();
      if (response.ok && data && data.type === 'SUCCESS') {
        // Update state in-place
        setAlerts(prev => prev.map(alert => alert.id === id ? { ...alert, status: 'RESOLVED' } : alert));
      } else {
        alert(data?.message || 'Eroare la rezolvarea alertei');
      }
    } catch (err) {
      console.error(err);
      // Local resolution in fallback mode
      setAlerts(prev => prev.map(alert => alert.id === id ? { ...alert, status: 'RESOLVED' } : alert));
    }
  };

  // Helper metadata styling based on emergency type
  const getServiceMeta = (type: string) => {
    switch (type.toUpperCase()) {
      case 'POLICE':
        return {
          label: 'Poliție / Securitate',
          icon: Shield,
          color: '#3B82F6', // Blue
          bgColor: 'rgba(59, 130, 246, 0.08)',
          borderColor: 'rgba(59, 130, 246, 0.2)'
        };
      case 'AMBULANCE':
        return {
          label: 'Ambulanță / Prim Ajutor',
          icon: HeartPulse,
          color: '#EF4444', // Red
          bgColor: 'rgba(239, 68, 68, 0.08)',
          borderColor: 'rgba(239, 68, 68, 0.2)'
        };
      case 'FIREFIGHTERS':
        return {
          label: 'Pompieri / Incendiu',
          icon: Flame,
          color: '#F97316', // Orange
          bgColor: 'rgba(249, 115, 22, 0.08)',
          borderColor: 'rgba(249, 115, 22, 0.2)'
        };
      default:
        return {
          label: type,
          icon: AlertTriangle,
          color: '#8B5CF6',
          bgColor: 'rgba(139, 92, 246, 0.08)',
          borderColor: 'rgba(139, 92, 246, 0.2)'
        };
    }
  };

  const filteredAlerts = alerts.filter(alert => {
    if (filter === 'ALL') return true;
    return alert.status === filter;
  });

  const pendingCount = alerts.filter(a => alertPending(a)).length;
  const resolvedCount = alerts.filter(a => a.status === 'RESOLVED').length;

  function alertPending(a: EmergencyAlert) {
    return a.status === 'PENDING' || a.status === 'pending';
  }

  return (
    <>
      {/* Page Header */}
      <div style={styles.pageHeader}>
        <div>
          <h2 style={styles.title}>Monitorizare Alerte de Urgență</h2>
          <p style={styles.subtitle}>Dispecerat în timp real pentru semnalările primite de la participanții la festival.</p>
        </div>
        
        <button 
          onClick={() => fetchAlerts(true)} 
          style={styles.refreshBtn} 
          disabled={isRefreshing}
          className="pulse-hover"
        >
          <RefreshCw size={14} className={isRefreshing ? 'animate-spin' : ''} />
          <span>{isRefreshing ? 'Se actualizează...' : 'Actualizează'}</span>
        </button>
      </div>

      {error && (
        <div style={styles.warningBanner}>
          <AlertTriangle size={18} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {/* Stats row */}
      <div style={styles.statsGrid}>
        <div style={styles.statCard} className="glass-card">
          <span style={styles.statLabel}>Total Semnalări</span>
          <span style={{ ...styles.statVal, color: 'var(--indigo-dark)' }}>{alerts.length}</span>
        </div>
        <div style={styles.statCard} className="glass-card">
          <span style={styles.statLabel}>Alerte Active</span>
          <span style={{ ...styles.statVal, color: '#EF4444' }}>{pendingCount}</span>
        </div>
        <div style={styles.statCard} className="glass-card">
          <span style={styles.statLabel}>Rezolvate</span>
          <span style={{ ...styles.statVal, color: '#10B981' }}>{resolvedCount}</span>
        </div>
      </div>

      {/* Filter Tabs */}
      <div style={styles.tabsRow}>
        <button 
          onClick={() => setFilter('ALL')} 
          style={{ ...styles.tab, ...(filter === 'ALL' ? styles.tabActive : {}) }}
        >
          Toate ({alerts.length})
        </button>
        <button 
          onClick={() => setFilter('PENDING')} 
          style={{ ...styles.tab, ...(filter === 'PENDING' ? styles.tabActive : {}) }}
        >
          Active / Pending ({pendingCount})
        </button>
        <button 
          onClick={() => setFilter('RESOLVED')} 
          style={{ ...styles.tab, ...(filter === 'RESOLVED' ? styles.tabActive : {}) }}
        >
          Rezolvate ({resolvedCount})
        </button>
      </div>

      {/* Alerts Grid */}
      {loading ? (
        <div style={styles.loadingContainer}>
          <RefreshCw className="animate-spin" size={32} style={{ color: 'var(--purple-primary)' }} />
          <p style={{ marginTop: '12px', fontWeight: 600, color: 'var(--text-muted)' }}>Se încarcă alertele...</p>
        </div>
      ) : filteredAlerts.length === 0 ? (
        <div style={styles.emptyContainer} className="glass-card">
          <CheckCircle size={48} style={{ color: '#10B981' }} />
          <h3 style={styles.emptyTitle}>Nicio alertă înregistrată</h3>
          <p style={styles.emptyText}>Toate zonele festivalului sunt în siguranță absolută.</p>
        </div>
      ) : (
        <div style={styles.alertsGrid}>
          {filteredAlerts.map((alert) => {
            const meta = getServiceMeta(alert.serviceType);
            const IconComponent = meta.icon;
            const isPending = alertPending(alert);
            
            return (
              <div 
                key={alert.id} 
                style={{ 
                  ...styles.alertCard, 
                  borderColor: isPending ? meta.color : 'rgba(93, 63, 211, 0.08)',
                  boxShadow: isPending ? `0 4px 20px ${meta.bgColor}` : 'none'
                }} 
                className="glass-card"
              >
                {/* Header card info */}
                <div style={styles.cardHeader}>
                  <div style={{ ...styles.iconContainer, backgroundColor: meta.bgColor, border: `1px solid ${meta.borderColor}` }}>
                    <IconComponent size={20} style={{ color: meta.color }} />
                  </div>
                  <div style={styles.headerInfo}>
                    <h4 style={styles.serviceTitle}>{meta.label}</h4>
                    <div style={styles.metaRow}>
                      <Clock size={12} style={{ color: 'var(--text-muted)' }} />
                      <span style={styles.metaText}>
                        {new Date(alert.createdAt).toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                      </span>
                    </div>
                  </div>
                  
                  {/* Status pill */}
                  <span style={{ 
                    ...styles.statusPill, 
                    backgroundColor: isPending ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
                    color: isPending ? '#EF4444' : '#10B981',
                    border: `1px solid ${isPending ? 'rgba(239, 68, 68, 0.2)' : 'rgba(16, 185, 129, 0.2)'}`
                  }}>
                    {isPending ? 'PENDING' : 'RESOLVED'}
                  </span>
                </div>

                {/* Location row */}
                <div style={styles.locationContainer}>
                  <MapPin size={16} style={{ color: 'var(--purple-primary)', flexShrink: 0 }} />
                  <div>
                    <span style={styles.locLabel}>Locație eveniment:</span>
                    <p style={styles.locName}>{alert.pinName}</p>
                  </div>
                </div>

                {/* Footer action button */}
                {isPending ? (
                  <button 
                    onClick={() => handleResolveAlert(alert.id)}
                    style={styles.resolveBtn}
                    className="pulse-hover"
                  >
                    <CheckCircle size={14} />
                    <span>Marchează ca Rezolvat</span>
                  </button>
                ) : (
                  <div style={styles.resolvedInfo}>
                    <CheckCircle size={14} style={{ color: '#10B981' }} />
                    <span style={{ color: '#10B981', fontWeight: 600, fontSize: '12px' }}>Alertă soluționată cu succes</span>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </>
  );
};

const styles: Record<string, React.CSSProperties> = {
  pageHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
  },
  title: {
    fontFamily: 'var(--font-heading)',
    fontSize: '24px',
    fontWeight: '800',
    color: 'var(--indigo-dark)',
  },
  subtitle: {
    fontSize: '13.5px',
    color: 'var(--text-muted)',
    fontWeight: '500',
    marginTop: '4px',
  },
  refreshBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '10px 16px',
    backgroundColor: 'var(--bg-sidebar-active)',
    color: 'var(--purple-primary)',
    border: '1px solid rgba(93, 63, 211, 0.08)',
    borderRadius: '12px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    fontFamily: 'var(--font-sans)',
    transition: 'var(--transition-fast)',
  },
  warningBanner: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '14px 20px',
    backgroundColor: 'rgba(245, 158, 11, 0.08)',
    border: '1px solid rgba(245, 158, 11, 0.15)',
    borderRadius: '12px',
    color: 'var(--text-main)',
    fontSize: '13px',
    fontWeight: '500',
    width: '100%',
    boxSizing: 'border-box',
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '20px',
    width: '100%',
  },
  statCard: {
    padding: '20px 24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  statLabel: {
    fontSize: '11px',
    fontWeight: '700',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
  },
  statVal: {
    fontSize: '28px',
    fontWeight: '800',
    fontFamily: 'var(--font-heading)',
  },
  tabsRow: {
    display: 'flex',
    gap: '8px',
    borderBottom: '1px solid rgba(93, 63, 211, 0.05)',
    paddingBottom: '2px',
    width: '100%',
  },
  tab: {
    padding: '8px 16px',
    backgroundColor: 'transparent',
    border: 'none',
    borderBottom: '2px solid transparent',
    color: 'var(--text-muted)',
    fontSize: '13.5px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'var(--transition-fast)',
    fontFamily: 'var(--font-sans)',
  },
  tabActive: {
    color: 'var(--purple-primary)',
    borderBottomColor: 'var(--purple-primary)',
    fontWeight: '700',
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '60px 0',
    width: '100%',
  },
  emptyContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '64px 32px',
    textAlign: 'center',
    width: '100%',
    maxWidth: '480px',
    margin: '0 auto',
    boxSizing: 'border-box',
  },
  emptyTitle: {
    fontFamily: 'var(--font-heading)',
    fontSize: '16.5px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
    marginTop: '16px',
  },
  emptyText: {
    fontSize: '13.5px',
    color: 'var(--text-muted)',
    fontWeight: '500',
    marginTop: '6px',
  },
  alertsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
    gap: '20px',
    width: '100%',
  },
  alertCard: {
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
    borderWidth: '1px',
    borderStyle: 'solid',
    transition: 'var(--transition-fast)',
  },
  cardHeader: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '12px',
    width: '100%',
    position: 'relative',
  },
  iconContainer: {
    width: '38px',
    height: '38px',
    borderRadius: '10px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  headerInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
    flex: 1,
    paddingRight: '75px', // space for absolute status pill
  },
  serviceTitle: {
    fontFamily: 'var(--font-heading)',
    fontSize: '15px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
  },
  metaRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
  },
  metaText: {
    fontSize: '11px',
    fontWeight: '600',
    color: 'var(--text-muted)',
  },
  statusPill: {
    position: 'absolute',
    right: 0,
    top: 0,
    fontSize: '9.5px',
    fontWeight: '800',
    padding: '4px 8px',
    borderRadius: '6px',
  },
  locationContainer: {
    display: 'flex',
    gap: '10px',
    alignItems: 'flex-start',
    backgroundColor: 'rgba(93, 63, 211, 0.02)',
    padding: '12px',
    borderRadius: '10px',
    border: '1px solid rgba(93, 63, 211, 0.03)',
  },
  locLabel: {
    fontSize: '11px',
    fontWeight: '700',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
  },
  locName: {
    fontSize: '13.5px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
    marginTop: '2px',
  },
  resolveBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    width: '100%',
    padding: '10px',
    backgroundColor: 'var(--purple-primary)',
    color: '#FFF',
    border: 'none',
    borderRadius: '10px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    fontFamily: 'var(--font-sans)',
    boxShadow: '0 4px 10px rgba(93, 63, 211, 0.1)',
  },
  resolvedInfo: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    width: '100%',
    padding: '10px',
    backgroundColor: 'rgba(16, 185, 129, 0.05)',
    border: '1px solid rgba(16, 185, 129, 0.1)',
    borderRadius: '10px',
  },
};

export default Alerts;
