import { test } from 'node:test'
import { strict as assert } from 'node:assert'
import { formatGraphTooltip } from '../src/utils/graphTooltip.js'

test('node name and node type cannot inject HTML or event handlers', () => {
  const tooltip = formatGraphTooltip(
    { dataType: 'node', data: { name: '<img src=x onerror="alert(1)">', _type: 'EVIL' } },
    { EVIL: '<script>alert(1)</script>' }, 'student'
  )
  assert.equal(tooltip, '<b>&lt;img src=x onerror=&quot;alert(1)&quot;&gt;</b><br/>类型：&lt;script&gt;alert(1)&lt;/script&gt;')
})

test('edge labels and normal Unicode names render safely', () => {
  assert.equal(formatGraphTooltip({ dataType: 'edge', data: { relType: '<svg/onload=alert(1)>' } }, {}, 'student'),
    '&lt;svg/onload=alert(1)&gt;')
  assert.equal(formatGraphTooltip({ dataType: 'node', data: { name: '数据结构 & 算法', _type: 'KP' } }, { KP: '知识点' }, 'admin'),
    '<b>数据结构 &amp; 算法</b><br/>知识点')
})
