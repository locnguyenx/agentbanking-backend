import React from 'react'

interface AuditLogEntry {
  auditId: string;
  serviceName: string;
  entityType: string;
  action: string;
  performedBy: string;
  ipAddress: string | null;
  timestamp: string;
  outcome: string;
  failureReason: string | null;
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
}

export const AuditLogTable: React.FC<AuditLogTableProps> = ({
  logs,
  isLoading,
  pagination,
  onPageChange,
}) => {
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
            <th style={{ textAlign: 'left', padding: '12px 8px', color: '#64748b', fontSize: '12px' }}>Failure Reason</th>
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
              <td style={{ padding: '12px 8px', fontSize: '13px', color: log.failureReason ? '#ef4444' : '#94a3b8' }}>
                {log.failureReason || '-'}
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
    </div>
  )
}
