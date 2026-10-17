import React, { useState, useEffect, useMemo } from 'react';
import { 
  Plus, 
  Trash2, 
  Pencil, 
  X, 
  RefreshCw, 
  Users, 
  Gauge, 
  ShieldAlert, 
  Radio, 
  CheckCircle2, 
  Send,
  Filter
} from 'lucide-react';
import { GenericTable } from '../components/GenericTable';
import type { Column } from '../components/GenericTable';

type DensityLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';

interface Pin {
  id: number;
  type: string;
  name: string;
}

interface CrowdReport {
  id: number;
  mapPinId: number;
  mapPinName: string;
  mapPinType: string;
  densityLevel: DensityLevel;
  reportedBy: string;
  notes: string | null;
  reportedAt: string;
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

const emptyForm = {
  mapPinId: '',
  densityLevel: 'LOW' as DensityLevel,
  reportedBy: '',
  notes: '',
};

const LEVEL_META: Record<DensityLevel, { label: string; color: string; bg: string }> = {
  LOW: { label: 'Low', color: '#1E9E6B', bg: 'rgba(30, 158, 107, 0.12)' },
  MODERATE: { label: 'Moderate', color: '#C79A1B', bg: 'rgba(199, 154, 27, 0.12)' },
  HIGH: { label: 'High Surge', color: '#E07A2C', bg: 'rgba(224, 122, 44, 0.18)' },
  CRITICAL: { label: 'Critical Threshold Breach', color: '#D64545', bg: 'rgba(214, 69, 69, 0.22)' },
};

export const CrowdReports = () => {
  const [reports, setReports] = useState<CrowdReport[]>([]);
  const [density, setDensity] = useState<ZoneDensity[]>([]);
  const [pins, setPins] = useState<Pin[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  // Security Dispatch Modal state
  const [dispatchModalOpen, setDispatchModalOpen] = useState(false);
  const [selectedAlertZone, setSelectedAlertZone] = useState<ZoneDensity | null>(null);
  const [stewardTeam, setStewardTeam] = useState('Alpha Patrol Team');
  const [dispatchNotes, setDispatchNotes] = useState('');
  const [dispatchSuccessMessage, setDispatchSuccessMessage] = useState('');

  // Auto-refresh & filtering
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [countdown, setCountdown] = useState(30);
  const [showThresholdOnly, setShowThresholdOnly] = useState(false);

  useEffect(() => {
    fetchAll();
  }, []);

  // 30-second auto-refresh timer for continuous operational security tracking
  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          fetchDensity();
          fetchReports();
          return 30;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [autoRefresh]);

  const fetchAll = async () => {
    setLoading(true);
    await Promise.all([fetchReports(), fetchDensity(), fetchPins()]);
    setLoading(false);
    setCountdown(30);
  };

  const fetchReports = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/crowd-reports');
      const data = await res.json();
      if (data && data.type === 'SUCCESS') {
        setReports(data.data);
      }
    } catch (err) {
      console.error('Failed to fetch crowd reports', err);
    }
  };

  const fetchDensity = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/crowd-reports/aggregation?windowMinutes=60');
      const data = await res.json();
      if (data && data.type === 'SUCCESS') {
        setDensity(data.data);
      }
    } catch (err) {
      console.error('Failed to fetch density aggregation', err);
    }
  };

  const fetchPins = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/map/active');
      const data = await res.json();
      if (data && data.type === 'SUCCESS' && data.data) {
        setPins(data.data.pins || []);
      }
    } catch (err) {
      console.error('Failed to fetch venue map pins', err);
    }
  };

  // Automated Security Threshold Breach Evaluation
  const alertZones = useMemo(() => {
    return density.filter((z) => z.currentLevel === 'HIGH' || z.currentLevel === 'CRITICAL');
  }, [density]);

  const displayedDensity = useMemo(() => {
    if (showThresholdOnly) {
      return density.filter((z) => z.currentLevel === 'HIGH' || z.currentLevel === 'CRITICAL');
    }
    return density;
  }, [density, showThresholdOnly]);

  const openCreateModal = () => {
    setEditingId(null);
    setForm(emptyForm);
    setShowModal(true);
  };

  const openEditModal = (report: CrowdReport) => {
    setEditingId(report.id);
    setForm({
      mapPinId: String(report.mapPinId),
      densityLevel: report.densityLevel,
      reportedBy: report.reportedBy || '',
      notes: report.notes || '',
    });
    setShowModal(true);
  };

  const openDispatchModal = (zone: ZoneDensity) => {
    setSelectedAlertZone(zone);
    setDispatchNotes(`Automated perimeter alert trigger at ${zone.mapPinName} (${zone.currentLevel} density). Stewards requested for crowd rerouting.`);
    setDispatchSuccessMessage('');
    setDispatchModalOpen(true);
  };

  const handleDispatchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAlertZone) return;
    setDispatchSuccessMessage(`Security Patrol dispatched to ${selectedAlertZone.mapPinName} successfully!`);
    setTimeout(() => {
      setDispatchModalOpen(false);
      setDispatchSuccessMessage('');
    }, 1800);
  };

  const handleFieldChange = (field: keyof typeof emptyForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.mapPinId || !form.reportedBy.trim()) return;

    setSaving(true);
    try {
      const url = editingId
        ? `http://localhost:8080/api/crowd-reports/${editingId}`
        : 'http://localhost:8080/api/crowd-reports';
      const res = await fetch(url, {
        method: editingId ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          mapPinId: Number(form.mapPinId),
          densityLevel: form.densityLevel,
          reportedBy: form.reportedBy,
          notes: form.notes,
        }),
      });
      const data = await res.json();
      if (data && data.type === 'SUCCESS') {
        setShowModal(false);
        fetchAll();
      } else {
        alert('Failed to save crowd report: ' + (data.message || 'Unknown error'));
      }
    } catch (err) {
      alert('Error saving crowd report: ' + err);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this crowd report?')) return;
    try {
      const res = await fetch(`http://localhost:8080/api/crowd-reports/${id}`, { method: 'DELETE' });
      const data = await res.json();
      if (data && data.type === 'SUCCESS') {
        fetchAll();
      } else {
        alert('Failed to delete crowd report: ' + (data.message || 'Unknown error'));
      }
    } catch (err) {
      alert('Error deleting crowd report: ' + err);
    }
  };

  const columns: Column<CrowdReport>[] = [
    {
      header: 'Zone',
      render: (r) => (
        <div>
          <div style={styles.nameText}>{r.mapPinName}</div>
          <div style={{ fontSize: '11px', color: 'var(--text-light)' }}>{r.mapPinType}</div>
        </div>
      ),
      style: { width: '22%' },
    },
    {
      header: 'Density',
      render: (r) => <DensityBadge level={r.densityLevel} />,
      style: { width: '18%' },
    },
    {
      header: 'Reported By',
      render: (r) => <span style={styles.cellText}>{r.reportedBy}</span>,
      style: { width: '18%' },
    },
    {
      header: 'Notes',
      render: (r) => <span style={styles.cellText}>{r.notes || '-'}</span>,
      style: { width: '23%' },
    },
    {
      header: 'Reported At',
      render: (r) => <span style={styles.cellText}>{new Date(r.reportedAt).toLocaleString()}</span>,
      style: { width: '14%' },
    },
    {
      header: 'Actions',
      render: (r) => (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '12px' }}>
          <button
            onClick={(e) => { e.stopPropagation(); openEditModal(r); }}
            style={styles.iconBtn}
            title="Edit report"
          >
            <Pencil size={16} />
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); handleDelete(r.id); }}
            style={styles.iconBtn}
            className="text-danger-hover"
            title="Delete report"
          >
            <Trash2 size={16} />
          </button>
        </div>
      ),
      style: { textAlign: 'right', width: '5%' },
      headerStyle: { textAlign: 'right' },
    },
  ];

  return (
    <>
      {/* Header */}
      <div style={styles.pageHeader}>
        <div>
          <h2 style={styles.title}>Crowd Reports & Security Perimeter Alerts</h2>
          <p style={styles.subtitle}>
            Real-time crowd density monitoring, venue perimeter threshold tracking, and automated security dispatch.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button
            onClick={() => setAutoRefresh(!autoRefresh)}
            style={{
              ...styles.filterBtn,
              backgroundColor: autoRefresh ? 'rgba(93, 63, 211, 0.1)' : '#FFF',
              color: autoRefresh ? 'var(--purple-primary)' : 'var(--text-muted)',
              borderColor: autoRefresh ? 'var(--purple-primary)' : 'var(--border-light)',
            }}
          >
            <Radio size={14} style={{ marginRight: '4px' }} className={autoRefresh ? 'animate-pulse' : ''} />
            {autoRefresh ? `Live Auto-Sync (${countdown}s)` : 'Auto-Sync Paused'}
          </button>

          <button style={styles.btnPrimary} className="pulse-hover" onClick={openCreateModal}>
            <Plus size={16} />
            <span>Add Log Report</span>
          </button>
        </div>
      </div>

      {/* Automated Security Perimeter Alert Banner (Appears when HIGH or CRITICAL density threshold breached) */}
      {alertZones.length > 0 && (
        <div style={styles.alertBanner} className="zoom-in-animation">
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={styles.alertIconBadge}>
              <ShieldAlert size={24} color="#FFF" />
            </div>
            <div>
              <div style={{ fontWeight: 800, fontSize: '15px', color: '#B91C1C', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span>AUTOMATED SECURITY PERIMETER ALERT</span>
                <span style={styles.alertCounterTag}>{alertZones.length} THRESHOLD BREACH{alertZones.length > 1 ? 'ES' : ''}</span>
              </div>
              <div style={{ fontSize: '13px', color: '#7F1D1D', marginTop: '2px' }}>
                High density detected at: <strong>{alertZones.map((z) => `${z.mapPinName} (${z.currentLevel})`).join(', ')}</strong>. Consider perimeter steward rerouting.
              </div>
            </div>
          </div>

          <button
            onClick={() => openDispatchModal(alertZones[0])}
            style={styles.btnDispatchAlert}
            className="pulse-hover"
          >
            <Send size={15} />
            <span>Dispatch Security Patrol</span>
          </button>
        </div>
      )}

      {/* Density Grid by Zone */}
      <div style={styles.card} className="glass-card">
        <div style={styles.cardHeader}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <h4 style={styles.cardTitle}>
              <Gauge size={18} style={{ marginRight: '8px', verticalAlign: 'middle', color: 'var(--purple-primary)' }} />
              Perimeter Zone Density <span style={{ fontWeight: 500, color: 'var(--text-light)', fontSize: '13px' }}>(Aggregated last 60 min)</span>
            </h4>
          </div>

          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <button
              onClick={() => setShowThresholdOnly(!showThresholdOnly)}
              style={{
                ...styles.filterBtn,
                backgroundColor: showThresholdOnly ? 'rgba(214, 69, 69, 0.12)' : '#FFF',
                color: showThresholdOnly ? '#D64545' : 'var(--text-muted)',
                borderColor: showThresholdOnly ? '#D64545' : 'var(--border-light)',
              }}
            >
              <Filter size={13} style={{ marginRight: '4px' }} />
              {showThresholdOnly ? 'Showing Threshold Alerts Only' : 'Filter High/Critical Only'}
            </button>

            <button style={styles.filterBtn} onClick={fetchAll}>
              <RefreshCw size={13} style={{ marginRight: '4px' }} /> Refresh
            </button>
          </div>
        </div>

        {displayedDensity.length === 0 ? (
          <p style={{ fontSize: '13px', color: 'var(--text-light)', padding: '16px 0' }}>
            {showThresholdOnly
              ? 'No perimeter threshold breaches detected. All venue zones within safe capacity.'
              : 'No crowd density reports logged in the last 60 minutes.'}
          </p>
        ) : (
          <div style={styles.densityGrid}>
            {displayedDensity.map((z) => {
              const meta = LEVEL_META[z.currentLevel];
              const isAlert = z.currentLevel === 'HIGH' || z.currentLevel === 'CRITICAL';

              return (
                <div
                  key={z.mapPinId}
                  style={{
                    ...styles.densityCard,
                    borderColor: meta.color,
                    boxShadow: isAlert ? `0 0 16px ${meta.color}35` : 'none',
                    backgroundColor: isAlert ? meta.bg : 'rgba(255, 255, 255, 0.7)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <div style={styles.nameText}>{z.mapPinName}</div>
                      <div style={{ fontSize: '11px', color: 'var(--text-light)', fontWeight: 600 }}>{z.mapPinType}</div>
                    </div>
                    <DensityBadge level={z.currentLevel} />
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '12px', fontSize: '12px', color: 'var(--text-muted)' }}>
                    <Users size={14} />
                    <span>{z.reportCount} report{z.reportCount === 1 ? '' : 's'} · score <strong>{z.averageScore.toFixed(1)}/4.0</strong></span>
                  </div>

                  <div style={{ fontSize: '11px', color: 'var(--text-light)', marginTop: '4px' }}>
                    Last logged by {z.lastReportedBy} · {new Date(z.lastReportedAt).toLocaleTimeString()}
                  </div>

                  {isAlert && (
                    <div style={{ marginTop: '12px', paddingTop: '10px', borderTop: '1px solid rgba(0,0,0,0.06)' }}>
                      <button
                        onClick={() => openDispatchModal(z)}
                        style={styles.btnQuickDispatch}
                      >
                        <Radio size={13} />
                        <span>Dispatch Stewards</span>
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Log Table */}
      <div style={{ ...styles.card, marginTop: '24px' }} className="glass-card">
        <div style={styles.cardHeader}>
          <h4 style={styles.cardTitle}>All Crowd Logs & Field Reports</h4>
        </div>

        {loading ? (
          <div style={{ padding: '40px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
            <RefreshCw size={24} className="animate-spin" style={{ color: 'var(--purple-primary)' }} />
            <span style={{ fontSize: '14px', color: 'var(--text-light)', fontWeight: 600 }}>Loading crowd reports...</span>
          </div>
        ) : (
          <GenericTable
            columns={columns}
            data={reports}
            itemsPerPage={8}
            emptyMessage="Nu există rapoarte de aglomerație. Apasă pe 'Add Log Report' pentru a adăuga primul raport!"
            flat={true}
          />
        )}
      </div>

      {/* Add / Edit Crowd Log Modal */}
      {showModal && (
        <div style={styles.modalOverlay}>
          <div style={styles.modalContent} className="glass-card zoom-in-animation">
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>{editingId ? 'Edit Crowd Report' : 'Add Crowd Report'}</h3>
              <button onClick={() => setShowModal(false)} style={styles.closeBtn}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} style={styles.modalBody}>
              <div>
                <label style={styles.fieldLabel}>Venue Zone / Map Pin *</label>
                <select
                  required
                  value={form.mapPinId}
                  onChange={(e) => handleFieldChange('mapPinId', e.target.value)}
                  style={styles.modalInputFull}
                >
                  <option value="">Select a zone</option>
                  {pins.map((p) => (
                    <option key={p.id} value={p.id}>{p.name} ({p.type})</option>
                  ))}
                </select>
                {pins.length === 0 && (
                  <p style={{ fontSize: '11px', color: 'var(--text-light)', marginTop: '4px' }}>
                    No venue map zones found — add pins in Venue Map first.
                  </p>
                )}
              </div>

              <div style={{ marginTop: '12px' }}>
                <label style={styles.fieldLabel}>Density Level *</label>
                <select
                  value={form.densityLevel}
                  onChange={(e) => handleFieldChange('densityLevel', e.target.value)}
                  style={styles.modalInputFull}
                >
                  <option value="LOW">Low (Fluid / 0-5 min wait)</option>
                  <option value="MODERATE">Moderate (Normal / 5-15 min wait)</option>
                  <option value="HIGH">High (Dense / 15-30 min wait)</option>
                  <option value="CRITICAL">Critical (Surge / Threshold Exceeded)</option>
                </select>
              </div>

              <div style={{ marginTop: '12px' }}>
                <label style={styles.fieldLabel}>Reported By (Staff / Steward Name) *</label>
                <input
                  type="text"
                  required
                  value={form.reportedBy}
                  onChange={(e) => handleFieldChange('reportedBy', e.target.value)}
                  style={styles.modalInputFull}
                  placeholder="Officer / Steward name"
                />
              </div>

              <div style={{ marginTop: '12px' }}>
                <label style={styles.fieldLabel}>Field Operational Notes</label>
                <textarea
                  value={form.notes}
                  onChange={(e) => handleFieldChange('notes', e.target.value)}
                  style={{ ...styles.modalInputFull, minHeight: '70px', resize: 'vertical' }}
                  placeholder="Observații privind fluxul, cozile de la bar sau perimetrul scenei..."
                />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  style={styles.btnCancel}
                  className="pulse-hover"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={{ ...styles.btnSubmit, flex: 1 }}
                  disabled={saving || !form.mapPinId || !form.reportedBy.trim()}
                >
                  {saving ? 'Saving...' : editingId ? 'Save Changes' : 'Log Crowd Report'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Dispatch Security Patrol Modal */}
      {dispatchModalOpen && selectedAlertZone && (
        <div style={styles.modalOverlay}>
          <div style={styles.modalContent} className="glass-card zoom-in-animation">
            <div style={styles.modalHeader}>
              <h3 style={{ ...styles.modalTitle, color: '#B91C1C', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ShieldAlert size={20} /> Dispatch Security Patrol
              </h3>
              <button onClick={() => setDispatchModalOpen(false)} style={styles.closeBtn}>
                <X size={20} />
              </button>
            </div>

            {dispatchSuccessMessage ? (
              <div style={{ padding: '30px', textAlign: 'center' }}>
                <CheckCircle2 size={48} color="#10B981" style={{ margin: '0 auto 12px auto' }} />
                <h4 style={{ fontSize: '16px', fontWeight: '800', color: '#065F46' }}>{dispatchSuccessMessage}</h4>
                <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Steward patrol has been dispatched and notified via radio feed.
                </p>
              </div>
            ) : (
              <form onSubmit={handleDispatchSubmit} style={styles.modalBody}>
                <div style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', padding: '14px', borderRadius: '12px', border: '1px solid rgba(239, 68, 68, 0.3)', marginBottom: '14px' }}>
                  <div style={{ fontSize: '14px', fontWeight: 800, color: '#991B1B' }}>
                    Target Zone: {selectedAlertZone.mapPinName}
                  </div>
                  <div style={{ fontSize: '12px', color: '#B91C1C', marginTop: '2px' }}>
                    Current Density: {selectedAlertZone.currentLevel} (Average Score: {selectedAlertZone.averageScore}/4.0)
                  </div>
                </div>

                <div>
                  <label style={styles.fieldLabel}>Assign Security / Steward Unit *</label>
                  <select
                    value={stewardTeam}
                    onChange={(e) => setStewardTeam(e.target.value)}
                    style={styles.modalInputFull}
                  >
                    <option value="Alpha Patrol Team">Alpha Patrol Team (Mainstage Perimeter)</option>
                    <option value="Bravo Crowd Control">Bravo Crowd Control (Food Courts & Bars)</option>
                    <option value="Charlie Rapid Response">Charlie Rapid Response (Gates A & B)</option>
                    <option value="Delta Medical Response">Delta First Aid & Medical Stewards</option>
                  </select>
                </div>

                <div style={{ marginTop: '12px' }}>
                  <label style={styles.fieldLabel}>Dispatch Radio Message / Instructions</label>
                  <textarea
                    value={dispatchNotes}
                    onChange={(e) => setDispatchNotes(e.target.value)}
                    style={{ ...styles.modalInputFull, minHeight: '80px', resize: 'vertical' }}
                  />
                </div>

                <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
                  <button
                    type="button"
                    onClick={() => setDispatchModalOpen(false)}
                    style={styles.btnCancel}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    style={{ ...styles.btnSubmit, backgroundColor: '#D64545', flex: 1 }}
                  >
                    Send Radio Dispatch
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </>
  );
};

const DensityBadge = ({ level }: { level: DensityLevel }) => {
  const meta = LEVEL_META[level];
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '4px 10px',
        borderRadius: '999px',
        fontSize: '11.5px',
        fontWeight: 700,
        color: meta.color,
        backgroundColor: meta.bg,
        textTransform: 'uppercase',
        letterSpacing: '0.03em',
      }}
    >
      {meta.label}
    </span>
  );
};

const styles: Record<string, React.CSSProperties> = {
  pageHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
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
  btnPrimary: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '10px 18px',
    backgroundColor: 'var(--coral-accent)',
    color: '#FFF',
    border: 'none',
    borderRadius: '12px',
    fontFamily: 'var(--font-sans)',
    fontWeight: '600',
    fontSize: '14px',
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(255, 111, 97, 0.2)',
    flexShrink: 0,
  },
  alertBanner: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px 20px',
    backgroundColor: 'rgba(254, 242, 242, 0.95)',
    border: '2px solid #FCA5A5',
    borderRadius: '16px',
    marginBottom: '24px',
    boxShadow: '0 10px 25px rgba(239, 68, 68, 0.15)',
  },
  alertIconBadge: {
    width: '42px',
    height: '42px',
    borderRadius: '12px',
    backgroundColor: '#DC2626',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  alertCounterTag: {
    fontSize: '10.5px',
    padding: '2px 8px',
    borderRadius: '999px',
    backgroundColor: '#EF4444',
    color: '#FFF',
    fontWeight: 800,
  },
  btnDispatchAlert: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '10px 16px',
    backgroundColor: '#DC2626',
    color: '#FFF',
    border: 'none',
    borderRadius: '10px',
    fontSize: '13px',
    fontWeight: 700,
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(220, 38, 38, 0.3)',
  },
  btnQuickDispatch: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '6px',
    padding: '8px 12px',
    backgroundColor: 'rgba(214, 69, 69, 0.12)',
    color: '#D64545',
    border: '1px solid rgba(214, 69, 69, 0.3)',
    borderRadius: '8px',
    fontSize: '12px',
    fontWeight: 700,
    cursor: 'pointer',
  },
  card: {
    padding: '28px',
    display: 'flex',
    flexDirection: 'column',
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  cardTitle: {
    fontFamily: 'var(--font-heading)',
    fontSize: '18px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
    display: 'flex',
    alignItems: 'center',
  },
  filterBtn: {
    backgroundColor: '#FFF',
    border: '1px solid rgba(93, 63, 211, 0.1)',
    borderRadius: '10px',
    padding: '6px 14px',
    fontSize: '12px',
    fontWeight: '600',
    color: 'var(--text-muted)',
    cursor: 'pointer',
    display: 'inline-flex',
    alignItems: 'center',
  },
  densityGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
    gap: '14px',
  },
  densityCard: {
    padding: '16px',
    borderRadius: '14px',
    border: '1.5px solid',
    transition: 'all 0.2s ease',
  },
  nameText: {
    fontSize: '14px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
  },
  cellText: {
    fontSize: '13px',
    color: 'var(--text-main)',
    fontWeight: '500',
  },
  iconBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--text-light)',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
  },
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(15, 12, 38, 0.4)',
    backdropFilter: 'blur(8px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  modalContent: {
    backgroundColor: '#FFF',
    borderRadius: '20px',
    width: '500px',
    maxHeight: '90vh',
    overflowY: 'auto',
    boxShadow: '0 20px 40px rgba(15, 12, 38, 0.15)',
    display: 'flex',
    flexDirection: 'column',
    border: '1px solid rgba(255, 255, 255, 0.6)',
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '24px 24px 16px 24px',
    borderBottom: '1px solid var(--border-light)',
  },
  modalTitle: {
    fontFamily: 'var(--font-heading)',
    fontSize: '18px',
    fontWeight: '800',
    color: 'var(--indigo-dark)',
  },
  closeBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--text-muted)',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '4px',
    borderRadius: '50%',
  },
  modalBody: {
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
  },
  fieldLabel: {
    display: 'block',
    fontSize: '12px',
    fontWeight: '600',
    color: 'var(--text-muted)',
    marginBottom: '6px',
  },
  modalInputFull: {
    width: '100%',
    padding: '10px 14px',
    borderRadius: '10px',
    border: '1px solid var(--border-light)',
    fontSize: '13px',
    outline: 'none',
    boxSizing: 'border-box',
    fontFamily: 'var(--font-sans)',
  },
  btnCancel: {
    flex: 1,
    padding: '12px',
    backgroundColor: '#FFF',
    color: 'var(--text-muted)',
    border: '1px solid var(--border-light)',
    borderRadius: '12px',
    fontFamily: 'var(--font-sans)',
    fontWeight: '600',
    fontSize: '14px',
    cursor: 'pointer',
  },
  btnSubmit: {
    padding: '11px',
    backgroundColor: 'var(--coral-accent)',
    color: '#FFF',
    border: 'none',
    borderRadius: '10px',
    fontSize: '13.5px',
    fontWeight: '600',
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(255, 111, 97, 0.15)',
  },
};

export default CrowdReports;
