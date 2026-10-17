import { useState, useEffect } from 'react';
import { 
  ShieldCheck, 
  Mail, 
  MapPin, 
  UserCheck, 
  Radio, 
  RefreshCw,
  Users,
  AlertCircle
} from 'lucide-react';
import { Announcements } from './Announcements';
import {type Column, GenericTable} from '../components/GenericTable';

interface StaffMember {
  id: number;
  name: string;
  role: string;
  dept: 'Operations' | 'Production' | 'Security & Access' | 'Infrastructure' | 'Medical';
  zone: string;
  status: 'Active' | 'On Break' | 'Standby' | 'Offline';
  avatar: string;
}

export const Staff = () => {
  const [activeSubTab, setActiveSubTab] = useState('Allocation');

  const subTabs = [
    { id: 'Allocation', label: 'Staff Allocation & Deployment' },
    { id: 'Announcements', label: 'Announcements Broadcaster' },
  ];

  // Live Roster and Stats States from Backend
  const [staffList, setStaffList] = useState<StaffMember[]>([]);
  const [stats, setStats] = useState({
    totalOnDuty: 0,
    activeZones: 0,
    radioStatus: 'Offline'
  });
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Reallocation States
  const [selectedStaffId, setSelectedStaffId] = useState<number>(0);
  const [targetZone, setTargetZone] = useState<string>('Main Stage Backstage');
  const [reallocateSuccess, setReallocateSuccess] = useState<string | null>(null);

  const [zonesList, setZonesList] = useState<string[]>([
    'Main Control Room',
    'Main Stage Backstage',
    'Cluj Arena Gate 1',
    'Central Park East Entrance',
    'Techno Room B',
    'First Aid Hub 3'
  ]);

  // Fetch staff roster and computed metrics from Spring Boot REST API
  const fetchStaffData = async (isInitial = false) => {
    try {
      if (isInitial) {
        setLoading(true);
      }
      const [staffRes, statsRes, mapRes] = await Promise.all([
        fetch('http://localhost:8080/api/staff'),
        fetch('http://localhost:8080/api/staff/stats'),
        fetch('http://localhost:8080/api/map/active')
      ]);

      if (!staffRes.ok) throw new Error(`Staff API error ${staffRes.status}`);
      if (!statsRes.ok) throw new Error(`Stats API error ${statsRes.status}`);

      const staffData = await staffRes.json();
      const statsData = await statsRes.json();

      const staffArray = staffData.data || [];
      // Sort deterministically by ID to prevent PostgreSQL MVCC row-reordering from scrambling the list or resetting pagination!
      staffArray.sort((a: any, b: any) => a.id - b.id);
      setStaffList(staffArray);
      setStats(statsData.data || { totalOnDuty: 0, activeZones: 0, radioStatus: 'Offline' });

      // Dynamically load deployment target zones from active map layout pins AND current staff roster zones!
      let combinedZones = [
        'Main Control Room',
        'Main Stage Backstage',
        'Cluj Arena Gate 1',
        'Central Park East Entrance',
        'Techno Room B',
        'First Aid Hub 3'
      ];

      if (mapRes.ok) {
        const mapJson = await mapRes.json();
        if (mapJson.data && mapJson.data.pins && mapJson.data.pins.length > 0) {
          const pinNames: string[] = mapJson.data.pins.map((pin: any) => pin.name);
          combinedZones = [...combinedZones, ...pinNames];
        }
      }

      if (staffArray.length > 0) {
        const staffZones = staffArray.map((s: any) => s.zone);
        combinedZones = [...combinedZones, ...staffZones];
      }

      // Deduplicate and filter out empty names
      const uniqueZones = Array.from(new Set(combinedZones)).filter(name => name && name.trim() !== '');
      if (uniqueZones.length > 0) {
        setZonesList(uniqueZones);
        // Set the active selected dropdown target if not already chosen or not in the new list
        if (!targetZone || !uniqueZones.includes(targetZone)) {
          setTargetZone(uniqueZones[0]);
        }
      } else {
        setTargetZone('Main Stage Backstage');
      }

      setError(null);
    } catch (err: any) {
      console.error('Failed to sync with the staff database', err);
      setError('Could not connect to the real-time staff dispatcher backend.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStaffData(true); // Initial load shows the beautiful loading spinner!
    // Keep the dispatcher panel alive with silent, real-time background status syncs!
    const interval = setInterval(() => {
      fetchStaffData(false);
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  // Set safe selected commander fallback when staffList changes
  useEffect(() => {
    if (staffList.length > 0 && !staffList.find(s => s.id === selectedStaffId)) {
      setSelectedStaffId(staffList[0].id);
    }
  }, [staffList, selectedStaffId]);

  const handleReallocate = async (e: React.FormEvent) => {
    e.preventDefault();
    const targetStaff = staffList.find(s => s.id === selectedStaffId);
    if (!targetStaff) return;

    try {
      const res = await fetch(`http://localhost:8080/api/staff/${selectedStaffId}/reallocate`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ zone: targetZone })
      });

      if (!res.ok) throw new Error('Deployment update failed');

      const apiRes = await res.json();
      
      // Update local roster list state
      if (apiRes.data) {
        setStaffList(prevList => 
          prevList.map(s => s.id === selectedStaffId ? apiRes.data : s)
        );
      }

      setReallocateSuccess(`Successfully deployed ${targetStaff.name} to ${targetZone}!`);
      
      // Re-fetch statistics to recalculate the widgets live
      const statsRes = await fetch('http://localhost:8080/api/staff/stats');
      if (statsRes.ok) {
        const statsData = await statsRes.json();
        setStats(statsData.data || { totalOnDuty: 0, activeZones: 0, radioStatus: 'Offline' });
      }

    } catch (err: any) {
      console.error(err);
      alert('Error updating deployment. Please check that your Spring Boot backend is active.');
    }
  };

  useEffect(() => {
    if (reallocateSuccess) {
      const timer = setTimeout(() => setReallocateSuccess(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [reallocateSuccess]);

  // Define Columns for the GenericTable
  const columns: Column<StaffMember>[] = [
    {
      header: 'Staff Member',
      render: (person) => (
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <h4 style={styles.name}>{person.name}</h4>
          <p style={styles.role}>{person.role}</p>
        </div>
      ),
      style: { width: '40%' }
    },
    {
      header: 'Department',
      render: (person) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <ShieldCheck size={14} style={{ color: 'var(--purple-primary)' }} />
          <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--text-main)' }}>{person.dept}</span>
        </div>
      ),
      style: { width: '25%' }
    },
    {
      header: 'Zone',
      render: (person) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <MapPin size={14} style={{ color: 'var(--coral-accent)' }} />
          <span style={{ fontSize: '13px', fontWeight: '500' }}>{person.zone}</span>
        </div>
      ),
      style: { width: '22%' }
    },
    {
      header: 'Status',
      render: (person) => (
        <span style={{ 
          ...styles.statusBadge, 
          backgroundColor: person.status === 'Active' ? 'rgba(34, 197, 94, 0.08)' : 
                           person.status === 'On Break' ? 'rgba(251, 191, 36, 0.08)' : 
                           person.status === 'Offline' ? 'rgba(107, 114, 128, 0.08)' : 'rgba(93, 63, 211, 0.08)',
          color: person.status === 'Active' ? 'var(--success)' : 
                 person.status === 'On Break' ? 'var(--warning)' : 
                 person.status === 'Offline' ? '#6B7280' : 'var(--purple-primary)'
        }}>
          {person.status}
        </span>
      ),
      style: { width: '13%', textAlign: 'center' }
    }
  ];

  return (
    <div style={styles.container}>
      {/* Horizontal Sub-tab bar */}
      <div style={styles.subTabBar}>
        {subTabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveSubTab(tab.id)}
            style={{
              ...styles.subTabButton,
              ...(activeSubTab === tab.id ? styles.subTabActive : {}),
            }}
          >
            {tab.label}
            {activeSubTab === tab.id && <div style={styles.subTabIndicator} />}
          </button>
        ))}
      </div>

      {activeSubTab === 'Announcements' ? (
        <div style={styles.contentBody}>
          <Announcements />
        </div>
      ) : (
        <>
          {/* 1. Header and Quick Metrics Row */}
          <div style={styles.header}>
            <div>
              <h2 style={styles.title}>Staff Allocation & Deployment Hub</h2>
              <p style={styles.subtitle}>Coordinate real-time zone placements, departments, and communication matrices across Cluj Central Park.</p>
            </div>
          </div>

          <div style={styles.metricsGrid}>
            <div style={styles.metricCard} className="glass-card">
              <div style={styles.metricHeader}>
                <div>
                  <p style={styles.metricLabel}>Total On-Duty</p>
                  <h4 style={styles.metricVal}>{stats.totalOnDuty} <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>personnel</span></h4>
                </div>
                <div style={{ ...styles.iconCircle, backgroundColor: 'rgba(93, 63, 211, 0.08)' }}>
                  <Users size={18} style={{ color: 'var(--purple-primary)' }} />
                </div>
              </div>
            </div>

            <div style={styles.metricCard} className="glass-card">
              <div style={styles.metricHeader}>
                <div>
                  <p style={styles.metricLabel}>Active Zones</p>
                  <h4 style={styles.metricVal}>{stats.activeZones} <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>monitored</span></h4>
                </div>
                <div style={{ ...styles.iconCircle, backgroundColor: 'rgba(255, 111, 97, 0.08)' }}>
                  <MapPin size={18} style={{ color: 'var(--coral-accent)' }} />
                </div>
              </div>
            </div>

            <div style={styles.metricCard} className="glass-card">
              <div style={styles.metricHeader}>
                <div>
                  <p style={styles.metricLabel}>Radio System</p>
                  <h4 style={{ ...styles.metricVal, color: stats.radioStatus === 'Optimal' ? 'var(--success)' : 'var(--warning)' }}>{stats.radioStatus}</h4>
                </div>
                <div style={{ ...styles.iconCircle, backgroundColor: stats.radioStatus === 'Optimal' ? 'rgba(34, 197, 94, 0.08)' : 'rgba(251, 191, 36, 0.08)' }}>
                  <Radio size={18} style={{ color: stats.radioStatus === 'Optimal' ? 'var(--success)' : 'var(--warning)' }} />
                </div>
              </div>
            </div>
          </div>

          {/* 2. Split Workspace Layout */}
          <div style={styles.splitLayout}>
            
            {/* Left Column: Command Roster (rendered using GenericTable.tsx) */}
            <div style={styles.leftCol} className="glass-card">
              <div style={styles.sectionTitleRow}>
                <h3 style={styles.sectionTitle}>Key Command Roster</h3>
                <span style={styles.statusPill}>Live Status Tracker</span>
              </div>

              {loading ? (
                <div style={styles.loadingContainer}>
                  <RefreshCw size={24} style={styles.spinner} />
                  <p style={{ marginTop: '8px', color: 'var(--text-muted)' }}>Syncing roster with main control room...</p>
                </div>
              ) : error ? (
                <div style={styles.emptyState}>
                  <AlertCircle size={32} style={{ color: 'var(--coral-accent)', marginBottom: '12px' }} />
                  <p style={{ fontWeight: '600', color: 'var(--indigo-dark)' }}>Failed to load roster</p>
                  <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginTop: '4px' }}>{error}</p>
                </div>
              ) : (
                <div style={{ width: '100%', overflowX: 'auto' }}>
                  <GenericTable<StaffMember>
                    columns={columns}
                    data={staffList}
                    itemsPerPage={5} // Keep it concise for side-by-side dashboard layout
                    emptyMessage="No active staff members registered"
                  />
                </div>
              )}
            </div>

            {/* Right Column: Deployment Control Deck */}
            <div style={styles.rightCol} className="glass-card">
              <h3 style={styles.sectionTitle}>Zone Allocation Control</h3>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '20px' }}>Reallocate resources and dispatch command personnel instantly to clear bottlenecks.</p>

              <form onSubmit={handleReallocate} style={styles.form}>
                <div style={styles.formGroup}>
                  <label style={styles.formLabel}>Select Commander</label>
                  <select 
                    value={selectedStaffId}
                    onChange={(e) => setSelectedStaffId(Number(e.target.value))}
                    style={styles.selectField}
                    disabled={loading || staffList.length === 0}
                  >
                    {staffList.length === 0 ? (
                      <option value="0">No commanders available</option>
                    ) : (
                      staffList.map(s => (
                        <option key={s.id} value={s.id}>{s.name} ({s.role})</option>
                      ))
                    )}
                  </select>
                </div>

                <div style={styles.formGroup}>
                  <label style={styles.formLabel}>Deployment Zone Target</label>
                  <select 
                    value={targetZone}
                    onChange={(e) => setTargetZone(e.target.value)}
                    style={styles.selectField}
                    disabled={loading}
                  >
                    {zonesList.map((z, idx) => (
                      <option key={idx} value={z}>{z}</option>
                    ))}
                  </select>
                </div>

                <button type="submit" style={styles.btnPrimary} className="pulse-hover" disabled={loading || staffList.length === 0}>
                  <RefreshCw size={15} className={loading ? "animate-spin" : ""} />
                  <span>Deploy & Sync Radio</span>
                </button>
              </form>

              {reallocateSuccess && (
                <div style={styles.successBanner}>
                  <UserCheck size={16} style={{ marginRight: '8px' }} />
                  <span>{reallocateSuccess}</span>
                </div>
              )}

              {/* Active crowds bottlenecks warning list */}
              <div style={styles.alertPanel}>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginBottom: '8px' }}>
                  <AlertCircle size={16} style={{ color: 'var(--coral-accent)' }} />
                  <h4 style={styles.alertTitle}>Resource Warnings</h4>
                </div>
                <p style={styles.alertDesc}>High check-in latency detected at <strong>Cluj Arena Gate 1</strong>. Standard response suggests reallocating Security or Operations officers to support local terminals.</p>
              </div>
            </div>

          </div>
        </>
      )}
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
    animation: 'fadeIn 0.3s ease-in-out',
    width: '100%',
  },
  subTabBar: {
    display: 'flex',
    gap: '24px',
    borderBottom: '1px solid var(--border-light)',
    paddingBottom: '2px',
    marginBottom: '8px',
  },
  subTabButton: {
    background: 'none',
    border: 'none',
    padding: '12px 4px 16px 4px',
    fontSize: '14.5px',
    fontWeight: '600',
    color: 'var(--text-light)',
    cursor: 'pointer',
    position: 'relative',
    fontFamily: 'var(--font-sans)',
    transition: 'var(--transition-fast)',
  },
  subTabActive: {
    color: 'var(--purple-primary)',
    fontWeight: '700',
  },
  subTabIndicator: {
    position: 'absolute',
    bottom: '-1px',
    left: 0,
    right: 0,
    height: '3px',
    backgroundColor: 'var(--purple-primary)',
    borderRadius: '3px 3px 0 0',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontFamily: 'var(--font-heading)',
    fontSize: '22px',
    fontWeight: '800',
    color: 'var(--indigo-dark)',
  },
  subtitle: {
    fontSize: '13.5px',
    color: 'var(--text-muted)',
    fontWeight: '500',
    marginTop: '4px',
    lineHeight: '1.4',
  },
  metricsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '20px',
  },
  metricCard: {
    padding: '20px',
  },
  metricHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  metricLabel: {
    fontSize: '12.5px',
    fontWeight: '600',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.3px',
  },
  metricVal: {
    fontSize: '20px',
    fontWeight: '800',
    color: 'var(--indigo-dark)',
    fontFamily: 'var(--font-heading)',
    marginTop: '4px',
  },
  iconCircle: {
    width: '36px',
    height: '36px',
    borderRadius: '10px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    transition: 'background-color 0.2s ease',
  },
  splitLayout: {
    display: 'flex',
    gap: '24px',
    width: '100%',
  },
  leftCol: {
    flex: 1.6,
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
    overflow: 'hidden',
  },
  rightCol: {
    flex: 1,
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
  },
  sectionTitleRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  sectionTitle: {
    fontSize: '16px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
    fontFamily: 'var(--font-heading)',
  },
  statusPill: {
    fontSize: '11px',
    fontWeight: '700',
    color: 'var(--purple-primary)',
    backgroundColor: 'rgba(93, 63, 211, 0.06)',
    padding: '4px 10px',
    borderRadius: '100px',
  },
  avatar: {
    width: '40px',
    height: '40px',
    borderRadius: '8px',
    objectFit: 'cover',
  },
  name: {
    fontFamily: 'var(--font-heading)',
    fontSize: '14px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
    lineHeight: '1.2',
  },
  role: {
    fontSize: '11px',
    color: 'var(--text-muted)',
    marginTop: '2px',
    fontWeight: '500',
  },
  statusBadge: {
    fontSize: '10.5px',
    fontWeight: '700',
    padding: '4px 8px',
    borderRadius: '6px',
    textTransform: 'uppercase',
    display: 'inline-block',
  },

  // Form styles
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
    marginTop: '12px',
  },
  formGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  formLabel: {
    fontSize: '12px',
    fontWeight: '600',
    color: 'var(--text-muted)',
  },
  selectField: {
    padding: '10px 14px',
    borderRadius: '8px',
    border: '1px solid var(--border-light)',
    fontFamily: 'var(--font-sans)',
    fontSize: '13.5px',
    fontWeight: '500',
    outline: 'none',
    boxSizing: 'border-box',
    width: '100%',
    backgroundColor: '#FFF',
    cursor: 'pointer',
  },
  btnPrimary: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    padding: '12px',
    backgroundColor: 'var(--purple-primary)',
    color: '#FFF',
    border: 'none',
    borderRadius: '10px',
    fontFamily: 'var(--font-sans)',
    fontWeight: '600',
    fontSize: '13.5px',
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(93, 63, 211, 0.15)',
    transition: 'var(--transition-fast)',
    marginTop: '8px',
  },
  successBanner: {
    display: 'flex',
    alignItems: 'center',
    padding: '10px 14px',
    borderRadius: '8px',
    backgroundColor: 'rgba(34, 197, 94, 0.08)',
    border: '1px solid rgba(34, 197, 94, 0.2)',
    color: '#15803D',
    fontSize: '12.5px',
    fontWeight: '600',
    marginTop: '16px',
  },
  alertPanel: {
    marginTop: 'auto',
    padding: '16px',
    borderRadius: '12px',
    backgroundColor: 'rgba(255, 111, 97, 0.04)',
    border: '1px solid rgba(255, 111, 97, 0.15)',
  },
  alertTitle: {
    fontSize: '13.5px',
    fontWeight: '700',
    color: 'var(--coral-accent)',
    fontFamily: 'var(--font-heading)',
  },
  alertDesc: {
    fontSize: '12px',
    color: 'var(--text-light)',
    lineHeight: '1.5',
    marginTop: '2px',
  },
  contentBody: {
    display: 'flex',
    flexDirection: 'column',
    width: '100%',
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 0',
    width: '100%',
  },
  emptyState: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 16px',
    textAlign: 'center',
    width: '100%',
  },
  spinner: {
    animation: 'spin 1s linear infinite',
    color: 'var(--purple-primary)',
  },
};

export default Staff;
