import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  X,
  Clock,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Play,
  RefreshCw,
  Settings,
  Download,
  Copy,
  ChevronDown,
  Activity,
  DollarSign,
  CreditCard,
  FileText,
  User,
  Hash,
  Calendar,
  ArrowRightLeft,
  Wallet,
  ExternalLink,
  FileCheck,
  Server,
  Bug,
  List
} from 'lucide-react'
import api from '../api/client'
import { WorkflowExecutionDetails, ExternalServiceStatus, WorkflowItem } from '../types/workflow'
import type { ActivityTimelineItem } from '../types/workflow'
import { ActivityStatus } from '../types/workflow'

interface WorkflowDetailsModalProps {
  workflow: WorkflowItem
  isOpen: boolean
  onClose: () => void
  onCreateCase?: () => void
  hasExistingCase?: boolean
  resolutionCase?: { caseId: string; status: string }
}

export const WorkflowDetailsModal: React.FC<WorkflowDetailsModalProps> = ({
  workflow,
  isOpen,
  onClose,
  onCreateCase,
  hasExistingCase = false,
  resolutionCase
}) => {
  const [activeTab, setActiveTab] = useState<'main' | 'execution'>('main')
  const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(false)
  const [expandedActivities, setExpandedActivities] = useState<Set<number>>(new Set())

  const {
    data: executionDetails,
    isLoading: executionLoading,
    error: executionError,
    refetch: refetchExecution
  } = useQuery({
    queryKey: ['workflowExecution', workflow.workflowId],
    queryFn: async () => {
      const response = await api.getWorkflowExecutionDetails(workflow.workflowId)
      return response as WorkflowExecutionDetails
    },
    refetchInterval: autoRefreshEnabled ? 5000 : false,
    enabled: isOpen && activeTab === 'execution',
    retry: 3,
    retryDelay: 1000
  })

  useEffect(() => {
    if (!isOpen) {
      setExpandedActivities(new Set())
      setActiveTab('main')
    }
  }, [isOpen])

  const getStatusConfig = (status: string) => {
    const configs: Record<string, { icon: React.ReactNode; bg: string; color: string; border: string; label: string }> = {
      'COMPLETED': { icon: <CheckCircle size={16} />, bg: '#ecfdf5', color: '#059669', border: '#a7f3d0', label: 'Completed' },
      'FAILED': { icon: <XCircle size={16} />, bg: '#fef2f2', color: '#dc2626', border: '#fecaca', label: 'Failed' },
      'PENDING': { icon: <Clock size={16} />, bg: '#fffbeb', color: '#d97706', border: '#fcd34d', label: 'Pending' },
      'RUNNING': { icon: <Play size={16} />, bg: '#eff6ff', color: '#2563eb', border: '#bfdbfe', label: 'Running' },
      'COMPENSATING': { icon: <Activity size={16} />, bg: '#eff6ff', color: '#2563eb', border: '#bfdbfe', label: 'Compensating' },
      'PENDING_REVIEW': { icon: <AlertTriangle size={16} />, bg: '#faf5ff', color: '#9333ea', border: '#e9d5ff', label: 'Pending Review' }
    }
    return configs[status] || configs['PENDING']
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

  const getTransactionIcon = (type: string) => {
    const icons: Record<string, React.ReactNode> = {
      'CASH_WITHDRAWAL': <DollarSign size={20} />,
      'RETAIL_SALE': <CreditCard size={20} />,
      'PREPAID_TOPUP': <Settings size={20} />,
      'BILL_PAYMENT': <FileText size={20} />,
      'DUIT_NOW_TRANSFER': <ArrowRightLeft size={20} />,
      'DEPOSIT': <Wallet size={20} />
    }
    return icons[type] || <Activity size={20} />
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

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('en-MY', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  }

  const toggleActivityExpansion = (sequence: number) => {
    setExpandedActivities(prev => {
      const newSet = new Set(prev)
      if (newSet.has(sequence)) newSet.delete(sequence)
      else newSet.add(sequence)
      return newSet
    })
  }

  const handleCopyDebugInfo = () => {
    if (executionDetails?.debugInfo) {
      navigator.clipboard.writeText(JSON.stringify(executionDetails.debugInfo, null, 2))
    }
  }

  const handleExportDebugInfo = () => {
    if (executionDetails?.debugInfo) {
      const blob = new Blob([JSON.stringify(executionDetails.debugInfo, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `workflow-${workflow.workflowId}-debug-${Date.now()}.json`
      a.click()
      URL.revokeObjectURL(url)
    }
  }

  if (!isOpen) return null

  const statusConfig = getStatusConfig(workflow.status)
  const caseStatusConfig = resolutionCase ? getCaseStatusConfig(resolutionCase.status) : null

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: 'rgba(0,0,0,0.6)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        backdropFilter: 'blur(4px)'
      }}
      onClick={onClose}
    >
      <div
        style={{
          width: '90vw',
          maxWidth: 900,
          maxHeight: '90vh',
          background: 'white',
          borderRadius: 16,
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden'
        }}
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{
          padding: '20px 24px',
          borderBottom: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexShrink: 0
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              width: 40,
              height: 40,
              borderRadius: 10,
              background: statusConfig.bg,
              color: statusConfig.color,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              {getTransactionIcon(workflow.transactionType)}
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: 18, fontWeight: 600, color: '#1e293b' }}>Workflow Details</h2>
              <span style={{ fontSize: 12, color: '#64748b', fontFamily: 'monospace' }}>
                {workflow.workflowId}
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}
          >
            <X size={20} color="#64748b" />
          </button>
        </div>

        {/* Tabs */}
        <div style={{
          display: 'flex',
          borderBottom: '1px solid #e2e8f0',
          flexShrink: 0
        }}>
          <button
            onClick={() => setActiveTab('main')}
            style={{
              padding: '12px 24px',
              border: 'none',
              background: activeTab === 'main' ? 'white' : '#f8fafc',
              borderBottom: activeTab === 'main' ? '2px solid #0d9488' : '2px solid transparent',
              color: activeTab === 'main' ? '#0d9488' : '#64748b',
              fontWeight: 600,
              fontSize: 14,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 8
            }}
          >
            <List size={16} />
            Main
          </button>
          <button
            onClick={() => setActiveTab('execution')}
            style={{
              padding: '12px 24px',
              border: 'none',
              background: activeTab === 'execution' ? 'white' : '#f8fafc',
              borderBottom: activeTab === 'execution' ? '2px solid #0d9488' : '2px solid transparent',
              color: activeTab === 'execution' ? '#0d9488' : '#64748b',
              fontWeight: 600,
              fontSize: 14,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 8
            }}
          >
            <Activity size={16} />
            Execution
            {autoRefreshEnabled && <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#10b981' }} />}
          </button>
          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#64748b', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={autoRefreshEnabled}
                onChange={(e) => setAutoRefreshEnabled(e.target.checked)}
                disabled={activeTab !== 'execution'}
              />
              Auto-refresh
            </label>
            {activeTab === 'execution' && (
              <button
                onClick={() => refetchExecution()}
                style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 4 }}
              >
                <RefreshCw size={14} color="#64748b" />
              </button>
            )}
          </div>
        </div>

        {/* Content */}
        <div style={{ flex: 1, overflow: 'auto', padding: 24 }}>
          {activeTab === 'main' ? (
            /* MAIN TAB - Workflow & Transaction Info */
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              {/* Transaction Type & Status */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontSize: 24, fontWeight: 700, color: '#1e293b' }}>
                    {getTransactionTypeLabel(workflow.transactionType)}
                  </div>
                  <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
                    Transaction Type
                  </div>
                </div>
                <span
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '8px 16px',
                    borderRadius: 8,
                    fontSize: 14,
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

              {/* Details Grid */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(2, 1fr)',
                gap: 16,
                background: '#f8fafc',
                padding: 20,
                borderRadius: 12
              }}>
                {/* Amount */}
                <div style={{ gridColumn: 'span 2', textAlign: 'center', padding: '12px 0', borderBottom: '1px solid #e2e8f0', marginBottom: 4 }}>
                  <div style={{ fontSize: 28, fontWeight: 700, color: '#1e293b' }}>
                    MYR {workflow.amount != null ? workflow.amount.toFixed(2) : '0.00'}
                  </div>
                  <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>Transaction Amount</div>
                </div>

                <DetailRow icon={<Hash size={14} />} label="Workflow ID" value={workflow.workflowId} mono />
                <DetailRow icon={<ExternalLink size={14} />} label="Transaction ID" value={workflow.transactionId} mono />
                <DetailRow icon={<Calendar size={14} />} label="Created At" value={formatDate(workflow.createdAt)} />
                <DetailRow icon={<User size={14} />} label="Agent ID" value={workflow.agentId || '-'} mono />
                <DetailRow icon={<DollarSign size={14} />} label="Customer Fee" value={workflow.customerFee != null ? `MYR ${workflow.customerFee.toFixed(2)}` : '-'} />
                <DetailRow icon={<FileText size={14} />} label="Reference Number" value={workflow.referenceNumber || '-'} mono />
                <DetailRow icon={<ExternalLink size={14} />} label="External Reference" value={workflow.externalReference || '-'} mono />
                <DetailRow icon={<Clock size={14} />} label="Completed At" value={workflow.completedAt ? formatDate(workflow.completedAt) : '-'} />
              </div>

              {/* Error/Reason Section */}
              {(workflow.pendingReason || workflow.errorCode || workflow.errorMessage) && (
                <div style={{
                  padding: 16,
                  background: '#fef2f2',
                  borderRadius: 12,
                  border: '1px solid #fecaca',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 12
                }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                    <AlertTriangle size={20} color="#dc2626" />
                    <div>
                      <div style={{ fontSize: 14, fontWeight: 600, color: '#991b1b' }}>Error / Pending Reason</div>
                      <div style={{ fontSize: 14, color: '#7f1d1d', marginTop: 4 }}>{workflow.pendingReason || workflow.errorMessage || '-'}</div>
                    </div>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12, paddingTop: 12, borderTop: '1px solid #fecaca' }}>
                    <DetailRow icon={<AlertTriangle size={14} color="#dc2626" />} label="Error Code" value={workflow.errorCode || '-'} />
                    <DetailRow icon={<FileText size={14} color="#dc2626" />} label="Error Details" value={workflow.errorDetails || '-'} />
                  </div>
                </div>
              )}

              {/* Resolution Case Section */}
              {resolutionCase && caseStatusConfig && (
                <div style={{
                  padding: 16,
                  background: '#f8fafc',
                  borderRadius: 12,
                  border: '1px solid #e2e8f0'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                    <FileCheck size={18} color="#64748b" />
                    <span style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>Resolution Case</span>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
                    <div>
                      <div style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase' }}>Case ID</div>
                      <div style={{ fontSize: 13, fontFamily: 'monospace', marginTop: 2 }}>{resolutionCase.caseId}</div>
                    </div>
                    <div>
                      <div style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase' }}>Status</div>
                      <span
                        style={{
                          display: 'inline-block',
                          marginTop: 2,
                          padding: '2px 8px',
                          borderRadius: 4,
                          fontSize: 12,
                          fontWeight: 600,
                          background: caseStatusConfig.bg,
                          color: caseStatusConfig.color
                        }}
                      >
                        {caseStatusConfig.label}
                      </span>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            /* EXECUTION TAB */
            executionLoading ? (
              <div style={{ textAlign: 'center', padding: 40 }}>
                <div style={{ width: 32, height: 32, border: '3px solid #e2e8f0', borderTopColor: '#0d9488', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px' }} />
                <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
                <p style={{ color: '#64748b' }}>Loading execution details...</p>
              </div>
            ) : executionError ? (
              <div style={{ textAlign: 'center', padding: 40 }}>
                <AlertTriangle size={48} color="#f59e0b" />
                <h3 style={{ color: '#1e293b', marginTop: 16 }}>Failed to Load Execution Details</h3>
                <p style={{ color: '#64748b' }}>{executionError instanceof Error ? executionError.message : 'Unknown error'}</p>
                <button
                  onClick={() => refetchExecution()}
                  className="btn btn-primary"
                  style={{ marginTop: 16 }}
                >
                  Retry
                </button>
              </div>
            ) : executionDetails ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
                {/* Execution Summary */}
                <ExecutionSummaryDetails details={executionDetails} />

                {/* Activity Timeline */}
                <div>
                  <h3 style={{ fontSize: 14, fontWeight: 600, color: '#334155', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Clock size={16} />
                    Activity Timeline
                  </h3>
                  {executionDetails.activityTimeline.length === 0 ? (
                    <div style={{ padding: 20, background: '#f8fafc', borderRadius: 8, textAlign: 'center', color: '#64748b' }}>
                      No activity timeline available
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {executionDetails.activityTimeline.map((activity) => (
                        <ActivityTimelineItemComponent
                          key={activity.sequence}
                          activity={activity}
                          isExpanded={expandedActivities.has(activity.sequence)}
                          onToggle={() => toggleActivityExpansion(activity.sequence)}
                        />
                      ))}
                    </div>
                  )}
                </div>

                {/* External Service Status */}
                <div>
                  <h3 style={{ fontSize: 14, fontWeight: 600, color: '#334155', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Server size={16} />
                    External Service Status
                  </h3>
                  <ExternalServiceStatusDisplay services={executionDetails.externalServiceStatus} />
                </div>

                {/* Debug Information */}
                <div>
                  <h3 style={{ fontSize: 14, fontWeight: 600, color: '#334155', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Bug size={16} />
                    Debug Information
                  </h3>
                  <DebugInfoPanel debugInfo={executionDetails.debugInfo} onCopy={handleCopyDebugInfo} onExport={handleExportDebugInfo} />
                </div>
              </div>
            ) : null
          )}
        </div>

        {/* Footer */}
        <div style={{
          padding: '16px 24px',
          borderTop: '1px solid #e2e8f0',
          display: 'flex',
          justifyContent: 'space-between',
          flexShrink: 0
        }}>
          <button className="btn btn-outline" onClick={onClose}>
            Close
          </button>
          {onCreateCase && !hasExistingCase && (
            <button className="btn btn-primary" onClick={onCreateCase}>
              Create Resolution Case
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

/* Sub-components */

const DetailRow: React.FC<{ icon: React.ReactNode; label: string; value: string; mono?: boolean }> = ({ icon, label, value, mono }) => (
  <div>
    <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginBottom: 4 }}>
      <span style={{ color: '#64748b' }}>{icon}</span>
      <span style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase' }}>{label}</span>
    </div>
    <div style={{ fontSize: 13, fontFamily: mono ? 'monospace' : 'inherit', color: '#1e293b' }}>{value}</div>
  </div>
)

const ExecutionSummaryDetails: React.FC<{ details: WorkflowExecutionDetails }> = ({ details }) => {
  const statusConfig: Record<string, { color: string; label: string }> = {
    'COMPLETED': { color: '#059669', label: 'Completed' },
    'FAILED': { color: '#dc2626', label: 'Failed' },
    'PENDING': { color: '#d97706', label: 'Pending' },
    'RUNNING': { color: '#2563eb', label: 'Running' }
  }
  const s = statusConfig[details.currentStatus] || { color: '#64748b', label: details.currentStatus }

  const completed = details.activityTimeline.filter(a => a.status === ActivityStatus.COMPLETED).length
  const failed = details.activityTimeline.filter(a => a.status === ActivityStatus.FAILED || a.status === ActivityStatus.TIMED_OUT).length

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: 16,
      background: '#f8fafc',
      borderRadius: 12
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <span style={{ fontSize: 24, fontWeight: 700, color: s.color }}>{s.label}</span>
        <span style={{ fontSize: 13, color: '#64748b' }}>Current Status</span>
      </div>
      <div style={{ display: 'flex', gap: 24 }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 18, fontWeight: 600, color: '#1e293b' }}>{details.activityTimeline.length}</div>
          <div style={{ fontSize: 11, color: '#64748b' }}>Total</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 18, fontWeight: 600, color: '#059669' }}>{completed}</div>
          <div style={{ fontSize: 11, color: '#64748b' }}>Completed</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 18, fontWeight: 600, color: '#dc2626' }}>{failed}</div>
          <div style={{ fontSize: 11, color: '#64748b' }}>Failed</div>
        </div>
      </div>
    </div>
  )
}

const ActivityTimelineItemComponent: React.FC<{
  activity: ActivityTimelineItem
  isExpanded: boolean
  onToggle: () => void
}> = ({ activity, isExpanded, onToggle }) => {
  const statusConfig: Record<string, { color: string; bg: string }> = {
    'COMPLETED': { color: '#10b981', bg: '#d1fae5' },
    'FAILED': { color: '#ef4444', bg: '#fee2e2' },
    'RUNNING': { color: '#f59e0b', bg: '#fef3c7' },
    'TIMED_OUT': { color: '#ef4444', bg: '#fee2e2' }
  }
  const s = statusConfig[activity.status] || { color: '#6b7280', bg: '#f3f4f6' }

  return (
    <div style={{
      border: '1px solid #e2e8f0',
      borderRadius: 8,
      overflow: 'hidden'
    }}>
      <div
        onClick={onToggle}
        style={{
          padding: '12px 16px',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          cursor: 'pointer',
          background: isExpanded ? '#f8fafc' : 'white'
        }}
      >
        <div style={{
          width: 24,
          height: 24,
          borderRadius: '50%',
          background: s.bg,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0
        }}>
          {activity.status === 'COMPLETED' ? <CheckCircle size={14} color={s.color} /> :
           activity.status === 'FAILED' ? <XCircle size={14} color={s.color} /> :
           <Clock size={14} color={s.color} />}
        </div>
        <span style={{ flex: 1, fontSize: 13, fontWeight: 500 }}>{activity.name}</span>
        <span style={{
          padding: '2px 8px',
          borderRadius: 4,
          fontSize: 11,
          fontWeight: 600,
          background: s.bg,
          color: s.color
        }}>
          {activity.status}
        </span>
        <span style={{ fontSize: 12, color: '#64748b' }}>{activity.duration}</span>
        <ChevronDown size={16} color="#64748b" style={{ transform: isExpanded ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }} />
      </div>
      {isExpanded && (
        <div style={{ padding: 12, background: '#f8fafc', borderTop: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column', gap: 8 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8, fontSize: 12 }}>
            <div><span style={{ color: '#64748b' }}>Sequence:</span> {activity.sequence}</div>
            <div><span style={{ color: '#64748b' }}>Start Time:</span> {activity.startTime ? new Date(activity.startTime).toLocaleString() : 'N/A'}</div>
          </div>
          {activity.pendingReason && (
            <div style={{ padding: 8, background: '#fef2f2', borderRadius: 4, fontSize: 12, color: '#dc2626' }}>
              {activity.pendingReason}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

const ExternalServiceStatusDisplay: React.FC<{ services: ExternalServiceStatus }> = ({ services }) => {
  const serviceList = [
    { key: 'rulesService', name: 'Rules Service' },
    { key: 'ledgerService', name: 'Ledger Service' },
    { key: 'switchAdapter', name: 'Switch Adapter' },
    { key: 'billerService', name: 'Biller Service' }
  ]

  const statusConfig: Record<string, { color: string; bg: string; label: string }> = {
    'AVAILABLE': { color: '#0ea5e9', bg: '#f0f9ff', label: 'Available' },
    'RESPONDING': { color: '#22c55e', bg: '#f0fdf4', label: 'Responding' },
    'TIMEOUT': { color: '#f59e0b', bg: '#fffbeb', label: 'Timeout' },
    'FAILED': { color: '#ef4444', bg: '#fef2f2', label: 'Failed' },
    'NOT_REQUIRED': { color: '#94a3b8', bg: '#f8fafc', label: 'Not Required' }
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
      {serviceList.map(service => {
        const status = services[service.key as keyof ExternalServiceStatus] || 'NOT_REQUIRED'
        const s = statusConfig[status] || statusConfig['NOT_REQUIRED']
        return (
          <div key={service.key} style={{
            padding: 12,
            background: s.bg,
            borderRadius: 8,
            border: `1px solid ${s.color}30`
          }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: s.color }}>{service.name}</div>
            <div style={{ fontSize: 11, color: s.color, marginTop: 2 }}>{s.label}</div>
          </div>
        )
      })}
    </div>
  )
}

const DebugInfoPanel: React.FC<{
  debugInfo: Record<string, any>
  onCopy: () => void
  onExport: () => void
}> = ({ debugInfo, onCopy, onExport }) => {
  if (!debugInfo) {
    return <div style={{ padding: 20, background: '#f8fafc', borderRadius: 8, textAlign: 'center', color: '#64748b' }}>No debug information available</div>
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', gap: 8 }}>
        <button onClick={onCopy} className="btn btn-outline btn-sm" style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          <Copy size={14} /> Copy
        </button>
        <button onClick={onExport} className="btn btn-outline btn-sm" style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          <Download size={14} /> Export
        </button>
      </div>
      <div style={{
        padding: 12,
        background: '#1e293b',
        borderRadius: 8,
        maxHeight: 200,
        overflow: 'auto'
      }}>
        <pre style={{ margin: 0, fontSize: 11, color: '#e2e8f0', fontFamily: 'monospace' }}>
          {JSON.stringify(debugInfo, null, 2)}
        </pre>
      </div>
    </div>
  )
}
