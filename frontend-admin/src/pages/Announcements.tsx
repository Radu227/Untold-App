import { useState, useEffect } from 'react';
import { Megaphone, Plus, Bell, Send, Radio, Loader2 } from 'lucide-react';
import { createPortal } from 'react-dom';

interface Announcement {
  id: number;
  title: string;
  content: string;
  targetAudience: string;
  createdAt: string;
}

export const Announcements = () => {
  const [history, setHistory] = useState<Announcement[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Form State
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [title, setTitle] = useState<string>('');
  const [content, setContent] = useState<string>('');
  const [targetAudience, setTargetAudience] = useState<string>('All Festival Attendees');
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Fetch broadcast history on mount
  const fetchHistory = async () => {
    try {
      setLoading(true);
      const res = await fetch('http://localhost:8080/api/announcements');
      if (!res.ok) throw new Error(`HTTP error ${res.status}`);
      const apiRes = await res.json();
      setHistory(apiRes.data || []);
      setError(null);
    } catch (err: any) {
      console.error('Failed to load announcement history', err);
      setError('Could not connect to the announcement server.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  const handleBroadcast = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    try {
      setSubmitting(true);
      const res = await fetch('http://localhost:8080/api/announcements', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: title.trim(),
          content: content.trim(),
          targetAudience: targetAudience
        })
      });

      if (!res.ok) throw new Error('Failed to send broadcast');
      
      const apiRes = await res.json();
      
      // Prepend to history local state
      if (apiRes.data) {
        setHistory(prev => [apiRes.data, ...prev]);
      }

      // Reset form & close modal
      setTitle('');
      setContent('');
      setTargetAudience('All Festival Attendees');
      setIsModalOpen(false);
    } catch (err: any) {
      console.error(err);
      alert('Error broadcasting announcement. Please check that the backend is running.');
    } finally {
      setSubmitting(false);
    }
  };

  const formatTimestamp = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      return d.toLocaleString('en-US', { 
        month: 'short', 
        day: 'numeric', 
        hour: '2-digit', 
        minute: '2-digit' 
      });
    } catch (e) {
      return dateStr;
    }
  };

  return (
    <>
      <div style={styles.header}>
        <div>
          <h2 style={styles.title}>Announcements Broadcaster</h2>
          <p style={styles.subtitle}>Send updates directly to all attendee mobile applications or staff channels.</p>
        </div>
        <button style={styles.btn} onClick={() => setIsModalOpen(true)} className="pulse-hover">
          <Plus size={16} />
          <span>New Broadcast</span>
        </button>
      </div>

      {error && (
        <div style={styles.errorAlert}>
          <Radio size={16} style={{ animation: 'pulse 1.5s infinite' }} />
          <span>{error}</span>
          <button style={styles.retryBtn} onClick={fetchHistory}>Retry Sync</button>
        </div>
      )}

      <div style={styles.card} className="glass-card">
        <h4 style={styles.cardTitle}>Recent Broadcast History</h4>
        
        {loading ? (
          <div style={styles.loadingContainer}>
            <Loader2 size={24} style={styles.spinner} />
            <p style={{ marginTop: '8px', color: 'var(--text-muted)' }}>Loading recent streams...</p>
          </div>
        ) : history.length === 0 ? (
          <div style={styles.emptyState}>
            <Megaphone size={32} style={{ color: 'var(--text-muted)', marginBottom: '12px' }} />
            <p style={{ fontWeight: '600', color: 'var(--indigo-dark)' }}>No broadcasts found</p>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginTop: '4px' }}>Compose your first announcement to transmit over Kafka!</p>
          </div>
        ) : (
          <div style={styles.historyList}>
            {history.map((announcement) => {
              const isStaff = announcement.targetAudience.toLowerCase().includes('staff');
              return (
                <div key={announcement.id} style={styles.item}>
                  {isStaff ? (
                    <Bell size={18} style={{ color: 'var(--coral-accent)' }} />
                  ) : (
                    <Megaphone size={18} style={{ color: 'var(--purple-primary)' }} />
                  )}
                  <div style={{ flex: 1 }}>
                    <h5 style={styles.itemTitle}>{announcement.title}</h5>
                    <p style={styles.itemBody}>{announcement.content}</p>
                    <p style={styles.itemDesc}>
                      Broadcasted to: <strong style={{ color: 'var(--indigo-dark)' }}>{announcement.targetAudience}</strong> • {formatTimestamp(announcement.createdAt)}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Modern Overlay Modal Dialog wrapped in React Portal */}
      {isModalOpen && createPortal(
        <div style={styles.modalOverlay} onClick={() => setIsModalOpen(false)}>
          <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h3 style={styles.modalHeader}>Initiate Broadcast</h3>

            <form onSubmit={handleBroadcast}>
              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Announcement Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g., Gate 1 Access Restricted"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  style={styles.formInput}
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Target Audience Channel</label>
                <select
                  value={targetAudience}
                  onChange={(e) => setTargetAudience(e.target.value)}
                  style={styles.formSelect}
                >
                  <option value="All Festival Attendees">All Festival Attendees</option>
                  <option value="Staff & Security Only">Staff & Security Only</option>
                  <option value="VIP Ticket Holders">VIP Ticket Holders</option>
                </select>
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Broadcast Message Body</label>
                <textarea
                  required
                  rows={4}
                  placeholder="Type the message body to push to devices..."
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  style={styles.formTextarea}
                />
              </div>

              <div style={styles.modalActions}>
                <button type="button" onClick={() => setIsModalOpen(false)} style={styles.btnCancel}>
                  Cancel
                </button>
                <button type="submit" disabled={submitting} style={styles.btnSave} className="pulse-hover">
                  {submitting ? (
                    <>
                      <Loader2 size={15} style={styles.spinner} />
                      <span>Transmitting...</span>
                    </>
                  ) : (
                    <>
                      <Send size={14} />
                      <span>Send Broadcast</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}
    </>
  );
};

const styles: Record<string, React.CSSProperties> = {
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
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
  },
  btn: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '10px 18px',
    backgroundColor: 'var(--purple-primary)',
    color: '#FFF',
    border: 'none',
    borderRadius: '12px',
    fontFamily: 'var(--font-sans)',
    fontWeight: '600',
    fontSize: '14px',
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(93, 63, 211, 0.2)',
    transition: 'var(--transition-fast)',
  },
  errorAlert: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px 16px',
    backgroundColor: 'rgba(239, 68, 68, 0.08)',
    border: '1px solid rgba(239, 68, 68, 0.2)',
    borderRadius: '12px',
    color: 'rgb(220, 38, 38)',
    fontSize: '14px',
    fontWeight: '500',
    marginBottom: '16px',
  },
  retryBtn: {
    marginLeft: 'auto',
    background: 'none',
    border: 'none',
    color: 'var(--purple-primary)',
    fontWeight: '600',
    cursor: 'pointer',
    textDecoration: 'underline',
  },
  card: {
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
    minHeight: '200px',
  },
  cardTitle: {
    fontFamily: 'var(--font-heading)',
    fontSize: '16.5px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
    marginBottom: '8px',
  },
  historyList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  item: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '16px',
    padding: '16px',
    borderRadius: '12px',
    backgroundColor: 'rgba(255, 255, 255, 0.4)',
    border: '1px solid rgba(93, 63, 211, 0.04)',
    transition: 'transform 0.2s',
  },
  itemTitle: {
    fontSize: '14.5px',
    fontWeight: '700',
    color: 'var(--indigo-dark)',
  },
  itemBody: {
    fontSize: '13.5px',
    color: 'var(--text-dark)',
    marginTop: '4px',
    lineHeight: '1.4',
  },
  itemDesc: {
    fontSize: '11.5px',
    color: 'var(--text-muted)',
    fontWeight: '500',
    marginTop: '6px',
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '40px 0',
  },
  emptyState: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '40px 16px',
    textAlign: 'center',
  },
  spinner: {
    animation: 'spin 1s linear infinite',
    color: 'var(--purple-primary)',
  },
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(30, 27, 75, 0.4)',
    backdropFilter: 'blur(8px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 9999,
  },
  modalContent: {
    width: '460px',
    padding: '32px',
    borderRadius: '24px',
    border: '1px solid var(--border-light)',
    backgroundColor: 'rgba(255, 255, 255, 0.98)',
    boxShadow: '0 20px 48px rgba(30, 27, 75, 0.15), 0 0 32px rgba(93, 63, 211, 0.05)',
    display: 'flex',
    flexDirection: 'column',
  },
  modalHeader: {
    fontFamily: 'var(--font-heading)',
    fontSize: '22px',
    fontWeight: '800',
    color: 'var(--indigo-dark)',
    marginBottom: '24px',
  },
  formGroup: {
    marginBottom: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  formLabel: {
    fontSize: '11px',
    fontWeight: '700',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
  },
  formInput: {
    padding: '12px 16px',
    borderRadius: '12px',
    backgroundColor: '#FFF',
    border: '1px solid rgba(93, 63, 211, 0.15)',
    color: 'var(--text-main)',
    fontSize: '14px',
    fontWeight: '500',
    outline: 'none',
    fontFamily: 'var(--font-sans)',
    transition: 'var(--transition-fast)',
  },
  formSelect: {
    padding: '12px 16px',
    borderRadius: '12px',
    backgroundColor: '#FFF',
    border: '1px solid rgba(93, 63, 211, 0.15)',
    color: 'var(--text-main)',
    fontSize: '14px',
    fontWeight: '500',
    outline: 'none',
    fontFamily: 'var(--font-sans)',
    transition: 'var(--transition-fast)',
  },
  formTextarea: {
    padding: '12px 16px',
    borderRadius: '12px',
    backgroundColor: '#FFF',
    border: '1px solid rgba(93, 63, 211, 0.15)',
    color: 'var(--text-main)',
    fontSize: '14px',
    fontWeight: '500',
    outline: 'none',
    fontFamily: 'var(--font-sans)',
    resize: 'none',
    transition: 'var(--transition-fast)',
  },
  modalActions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '12px',
    marginTop: '20px',
  },
  btnCancel: {
    padding: '10px 18px',
    borderRadius: '12px',
    backgroundColor: '#F3F4F6',
    border: 'none',
    color: 'var(--text-dark)',
    fontWeight: '600',
    fontSize: '14px',
    cursor: 'pointer',
    fontFamily: 'var(--font-sans)',
    transition: 'var(--transition-fast)',
  },
  btnSave: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '10px 18px',
    borderRadius: '12px',
    backgroundColor: 'var(--purple-primary)',
    border: 'none',
    color: '#FFF',
    fontWeight: '600',
    fontSize: '14px',
    cursor: 'pointer',
    fontFamily: 'var(--font-sans)',
    transition: 'var(--transition-fast)',
  },
};

