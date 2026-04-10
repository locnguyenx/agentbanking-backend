import React, { useState } from 'react'

interface AuditLogEntry {
  auditId: string;
  serviceName: string;
  entityType: string;
  entityId: string | null;
  action: string;
  performedBy: string;
  ipAddress: string | null;
  timestamp: string;
  outcome: string;
  failureReason: string | null;
  changes: string | null;
  traceId: string | null;
  sessionId: string | null;
  deviceInfo: string | null;
  geographicLocation: string | null;
}

interface Pagination {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface AuditLogTableProps {
  logs: AuditLogEntry[];
  isLoading?: boolean;
  pagination?: Pagination;
  onPageChange?: (page: number) => void;
  onView?: (log: AuditLogEntry) => void;
}

export const AuditLogTable: React.FC<AuditLogTableProps> = ({
  logs,
  isLoading,
  pagination,
  onPageChange,
  onView,
}) => {
  const [selectedLog, setSelectedLog] = useState<AuditLogEntry | null>(null)

  if (isLoading) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '40px' }}>
        Loading audit logs...
      </div>
    )
  }

  if (!logs || logs.length === 0) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '40px' }}>
        <p style={{ color: '#64748b' }}>No audit logs found</p>
      </div>
    )
  }

  return (
    <div className="card" style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #e2e8f0' }}>
            <th style={{ textAlign: 'left', padding: '12px 8px', color: '#64748b', fontSize: '12px' }}>Timestamp</th>
            <th style={{ textAlign: 'left', padding: '12px 8px', color: '#64748b', fontSize: '12px' }}>Service</th>
            <th style={{ textAlign: 'left', padding: '12px 8px', color: '#64748b', fontSize: '12px' }}>Action</th>
            <th style={{ textAlign: 'left', padding: '12px 8px', color: '#64748b', fontSize: '12px' }}>User</th>
            <th style={{ textAlign: 'left', padding: '12px 8px', color: '#64748b', fontSize: '12px' }}>Entity</th>
            <th style={{ textAlign: 'left', padding: '12px 8px', color: '#64748b', fontSize: '12px' }}>Outcome</th>
            <th style={{ textAlign: 'left', padding: '12px 8px', color: '#64748b', fontSize: '12px' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log) => (
            <tr key={log.auditId} style={{ borderBottom: '1px solid #f1f5f9' }}>
              <td style={{ padding: '12px 8px', fontSize: '13px' }}>
                {new Date(log.timestamp).toLocaleString()}
              </td>
              <td style={{ padding: '12px 8px', fontSize: '13px' }}>
                {log.serviceName}
              </td>
              <td style={{ padding: '12px 8px', fontSize: '13px' }}>
                <span style={{ 
                  background: '#f1f5f9', 
                  padding: '2px 8px', 
                  borderRadius: '4px',
                  fontSize: '12px',
                  fontFamily: 'monospace'
                }}>
                  {log.action}
                </span>
              </td>
              <td style={{ padding: '12px 8px', fontSize: '13px' }}>{log.performedBy}</td>
              <td style={{ padding: '12px 8px', fontSize: '13px' }}>{log.entityType}</td>
              <td style={{ padding: '12px 8px' }}>
                <span className={`badge ${log.outcome === 'SUCCESS' ? 'badge-success' : 'badge-error'}`}>
                  {log.outcome}
                </span>
              </td>
              <td style={{ padding: '12px 8px' }}>
                <button
                  onClick={() => {
                    setSelectedLog(log)
                    onView?.(log)
                  }}
                  style={{ 
                    backgroundColor: '#1e3a5f', 
                    color: 'white', 
                    border: 'none', 
                    padding: '6px 16px', 
                    borderRadius: '4px', 
                    fontSize: '12px',
                    cursor: 'pointer',
                    fontWeight: 500,
                  }}
                >
                  View
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {pagination && pagination.totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px' }}>
          <div style={{ fontSize: '14px', color: '#64748b' }}>
            Showing {pagination.page * pagination.size + 1} to {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of {pagination.totalElements}
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              onClick={() => onPageChange?.(pagination.page - 1)}
              disabled={pagination.page === 0}
              className="btn btn-secondary"
              style={{ padding: '6px 12px' }}
            >
              Previous
            </button>
            <span style={{ padding: '6px 12px', fontSize: '14px' }}>
              Page {pagination.page + 1} of {pagination.totalPages}
            </span>
            <button
              onClick={() => onPageChange?.(pagination.page + 1)}
              disabled={pagination.page >= pagination.totalPages - 1}
              className="btn btn-secondary"
              style={{ padding: '6px 12px' }}
            >
              Next
            </button>
          </div>
        </div>
      )}

      {selectedLog && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
        }} onClick={() => setSelectedLog(null)}>
          <div style={{
            backgroundColor: 'white',
            borderRadius: '8px',
            padding: '24px',
            maxWidth: '600px',
            width: '90%',
            maxHeight: '80vh',
            overflow: 'auto',
          }} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Audit Log Details</h3>
              <button
                onClick={() => setSelectedLog(null)}
                style={{ background: 'none', border: 'none', fontSize: '24px', cursor: 'pointer', color: '#64748b' }}
              >
                ×
              </button>
            </div>
            
            <div style={{ display: 'grid', gap: '12px' }}>
              <DetailRow label="Audit ID" value={selectedLog.auditId} />
              <DetailRow label="Timestamp" value={new Date(selectedLog.timestamp).toLocaleString()} />
              <DetailRow label="Service" value={selectedLog.serviceName} />
              <DetailRow label="Action" value={selectedLog.action} />
              <DetailRow label="Performed By" value={selectedLog.performedBy} />
              <DetailRow label="Entity Type" value={selectedLog.entityType} />
              <DetailRow label="Entity ID" value={selectedLog.entityId || '-'} />
              <DetailRow label="Outcome" value={selectedLog.outcome} />
              <DetailRow label="IP Address" value={selectedLog.ipAddress || '-'} />
              <DetailRow label="Trace ID" value={selectedLog.traceId || '-'} />
              <DetailRow label="Session ID" value={selectedLog.sessionId || '-'} />
              <DetailRow label="Device Info" value={selectedLog.deviceInfo || '-'} />
              <DetailRow label="Geographic Location" value={selectedLog.geographicLocation || '-'} />
              <DetailRow label="Failure Reason" value={selectedLog.failureReason || '-'} isError />
              <DetailRow label="Changes" value={selectedLog.changes || '-'} isMonospace />
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

const DetailRow: React.FC<{ label: string; value: string; isError?: boolean; isMonospace?: boolean }> = ({ 
  label, 
  value, 
  isError,
  isMonospace 
}) => (
  <div style={{ display: 'grid', gridTemplateColumns: '180px 1fr', gap: '8px', padding: '8px 0', borderBottom: '1px solid #f1f5f9' }}>
    <span style={{ color: '#64748b', fontSize: '13px', fontWeight: 500 }}>{label}</span>
    <span style={{ 
      color: isError ? '#ef4444' : '#1e293b', 
      fontSize: '13px',
      fontFamily: isMonospace ? 'monospace' : 'inherit',
      wordBreak: 'break-word',
    }}>
      {value}
    </span>
  </div>
)
