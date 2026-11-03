/** ECharts HTML tooltips use innerHTML, so every dynamic value must be escaped. */
export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character])
}

export function formatGraphTooltip(params, types, view, edgeLabel = (value) => value) {
  if (params.dataType !== 'node') {
    return view === 'student' ? escapeHtml(edgeLabel(params.data?.relType)) : ''
  }
  const name = escapeHtml(params.data?.name)
  const type = escapeHtml(types[params.data?._type] || '')
  return `<b>${name}</b><br/>${view === 'student' ? '类型：' : ''}${type}`
}
