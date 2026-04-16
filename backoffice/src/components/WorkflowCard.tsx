import { 
  Clock, 
  CheckCircle, 
  XCircle, 
  AlertTriangle, 
  Play, 
  CreditCard,
  DollarSign, 
  ArrowRightLeft, 
  Settings,
  FileText,
  User,
  Hash,
  Wallet,
  Calendar,
  Activity,
  ExternalLink,
  ChevronRight
} from 'lucide-react'
import { WorkflowItem } from '../types/workflow'

interface WorkflowCardProps {
  workflow: WorkflowItem
  resolutionCase?: { caseId: string; status: string }
  onViewDetails: (workflow: WorkflowItem) => void
  onCreateCase: (workflow: WorkflowItem) => void
}

export const WorkflowCard: React.FC<WorkflowCardProps> = ({
  workflow,
  resolutionCase,
  onViewDetails,
  onCreateCase
}) => {
  const getTransactionIcon = (type: string) => {
    const icons: Record<string, React.ReactNode> = {
      'CASH_WITHDRAWAL': <DollarSign size={24} />,
      'RETAIL_SALE': <CreditCard size={24} />,
      'PREPAID_TOPUP': <Settings size={24} />,
      'BILL_PAYMENT': <FileText size={24} />,
      'DUIT_NOW_TRANSFER': <ArrowRightLeft size={24} />,
      'DEPOSIT': <Wallet size={24} />
    }
    return icons[type] || <Activity size={24} />
  }

  const getTransactionTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      'CASH_WITHDRAWAL': 'Cash Withdrawal',
      'RETAIL_SALE': 'Retail Sale',
      'PREPAID_TOPUP': 'Prepaid Topup',
      'BILL_PAYMENT': 'Bill Payment',
      'DUIT_NOW_TRANSFER': 'DuitNow Transfer',
      'DEPOSIT': 'Deposit'
    }
    return labels[type] || type.replace(/_/g, ' ')
  }

  const getStatusConfig = (status: string) => {
    const configs: Record<string, { 
      icon: React.ReactNode
      bg: string
      color: string
      border: string
      label: string 
    }> = {
      'COMPLETED': { 
        icon: <CheckCircle size={16} />,
        bg: '#ecfdf5',
        color: '#059669',
        border: '#a7f3d0',
        label: 'Completed'
      },
      'FAILED': { 
        icon: <XCircle size={16} />,
        bg: '#fef2f2',
        color: '#dc2626',
        border: '#fecaca',
        label: 'Failed'
      },
      'PENDING': { 
        icon: <Clock size={16} />,
        bg: '#fffbeb',
        color: '#d97706',
        border: '#fcd34d',
        label: 'Pending'
      },
      'RUNNING': { 
        icon: <Play size={16} />,
        bg: '#eff6ff',
        color: '#2563eb',
        border: '#bfdbfe',
        label: 'Running'
      },
      'COMPENSATING': { 
        icon: <Activity size={16} />,
        bg: '#eff6ff',
        color: '#2563eb',
        border: '#bfdbfe',
        label: 'Compensating'
      },
      'PENDING_REVIEW': { 
        icon: <AlertTriangle size={16} />,
        bg: '#faf5ff',
        color: '#9333ea',
        border: '#e9d5ff',
        label: 'Pending Review'
      }
    }
    return configs[status] || configs['PENDING']
  }

  const getCaseStatusConfig = (status: string) => {
    const configs: Record<string, { bg: string; color: string; label: string }> = {
      'PENDING_MAKER': { bg: '#fef3c7', color: '#d97706', label: 'Pending Maker' },
      'PENDING_CHECKER': { bg: '#dbeafe', color: '#2563eb', label: 'Pending Checker' },
      'APPROVED': { bg: '#d1fae5', color: '#059669', label: 'Approved' },
      'REJECTED': { bg: '#fee2e2', color: '#dc2626', label: 'Rejected' }
    }
    return configs[status] || { bg: '#f3f4f6', color: '#6b7280', label: status }
  }

  const statusConfig = getStatusConfig(workflow.status)
  const caseStatusConfig = resolutionCase ? getCaseStatusConfig(resolutionCase.status) : null

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleString('en-MY', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const formatId = (id: string, maxLength: number = 20) => {
    if (!id) return 'N/A'
    if (id.length <= maxLength) return id
    return `${id.substring(0, 8)}...${id.substring(id.length - 8)}`
  }

  return (
    <div 
      className="workflow-card-modern"
      style={{
        background: 'white',
        borderRadius: '12px',
        border: '1px solid #e2e8f0',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
        padding: '20px',
        marginBottom: '16px',
        transition: 'all 0.2s ease',
        cursor: 'pointer'
      }}
      onClick={() => onViewDetails(workflow)}
    >
      {/* Header Section */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flex: 1 }}>
          {/* Transaction Type Icon */}
          <div 
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '12px',
              background: statusConfig.bg,
              color: statusConfig.color,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}
          >
            {getTransactionIcon(workflow.transactionType)}
          </div>
          
          {/* Transaction Info */}
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
              <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600, color: '#1e293b' }}>
                {getTransactionTypeLabel(workflow.transactionType)}
              </h3>
              <span 
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  fontSize: '12px',
                  fontWeight: 600,
                  background: statusConfig.bg,
                  color: statusConfig.color,
                  border: `1px solid ${statusConfig.border}`
                }}
              >
                {statusConfig.icon}
                {statusConfig.label}
              </span>
            </div>
            
            {/* IDs Row */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Hash size={12} color="#64748b" />
                <span style={{ fontSize: '12px', color: '#64748b' }}>Workflow:</span>
                <span style={{ fontSize: '12px', color: '#334155', fontFamily: 'monospace' }}>
                  {formatId(workflow.workflowId)}
                </span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <ExternalLink size={12} color="#64748b" />
                <span style={{ fontSize: '12px', color: '#64748b' }}>Transaction:</span>
                <span style={{ fontSize: '12px', color: '#334155', fontFamily: 'monospace' }}>
                  {formatId(workflow.transactionId)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Amount */}
        <div style={{ textAlign: 'right', marginLeft: '16px' }}>
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#1e293b' }}>
            MYR {workflow.amount?.toFixed(2) || '0.00'}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
            Amount
          </div>
        </div>
      </div>

      {/* Details Section */}
      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        gap: '24px',
        padding: '12px 16px',
        background: '#f8fafc',
        borderRadius: '8px',
        marginBottom: '12px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Calendar size={14} color="#64748b" />
          <span style={{ fontSize: '13px', color: '#64748b' }}>Created:</span>
          <span style={{ fontSize: '13px', color: '#334155' }}>
            {formatDate(workflow.createdAt)}
          </span>
        </div>
        
        {workflow.agentId && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <User size={14} color="#64748b" />
            <span style={{ fontSize: '13px', color: '#64748b' }}>Agent:</span>
            <span style={{ fontSize: '13px', color: '#334155', fontFamily: 'monospace' }}>
              {formatId(workflow.agentId, 12)}
            </span>
          </div>
        )}

        {resolutionCase && caseStatusConfig && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginLeft: 'auto' }}>
            <span style={{ fontSize: '13px', color: '#64748b' }}>Case:</span>
            <span 
              style={{
                padding: '2px 8px',
                borderRadius: '4px',
                fontSize: '11px',
                fontWeight: 600,
                background: caseStatusConfig.bg,
                color: caseStatusConfig.color
              }}
            >
              {caseStatusConfig.label}
            </span>
          </div>
        )}
      </div>

      {/* Error Reason Section (only if failed/pending) */}
      {workflow.pendingReason && (
        <div style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: '8px',
          padding: '12px 16px',
          background: '#fef2f2',
          borderRadius: '8px',
          marginBottom: '12px',
          border: '1px solid #fecaca'
        }}>
          <AlertTriangle size={16} color="#dc2626" style={{ marginTop: '2px', flexShrink: 0 }} />
          <div>
            <div style={{ fontSize: '12px', fontWeight: 600, color: '#991b1b', marginBottom: '2px' }}>
              Error / Pending Reason
            </div>
            <div style={{ fontSize: '13px', color: '#7f1d1d' }}>
              {workflow.pendingReason}
            </div>
          </div>
        </div>
      )}

      {/* Footer Section with Actions */}
      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        borderTop: '1px solid #e2e8f0',
        paddingTop: '12px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '12px', color: '#64748b' }}>Actions:</span>
          {(() => {
            const needsResolution = workflow.status === 'COMPENSATING' || 
              (workflow.status === 'PENDING' && !!workflow.pendingReason);
            if (!needsResolution) {
              return null;
            }
            return !resolutionCase ? (
              <button 
                className="btn btn-primary btn-sm"
                onClick={(e) => {
                  e.stopPropagation()
                  onCreateCase(workflow)
                }}
                style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '6px',
                  padding: '6px 12px'
                }}
              >
                <AlertTriangle size={14} />
                Create Case
              </button>
            ) : (
              <span style={{ fontSize: '12px', color: '#059669', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <CheckCircle size={14} />
                Case created
              </span>
            );
          })()}
        </div>
        
        <button 
          className="btn btn-outline btn-sm"
          onClick={(e) => {
            e.stopPropagation()
            onViewDetails(workflow)
          }}
          style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '6px',
            padding: '6px 12px'
          }}
        >
          View Details
          <ChevronRight size={14} />
        </button>
      </div>
    </div>
  )
}
