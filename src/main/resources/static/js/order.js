/* 订单管理：查询、详情与最小状态机。依赖 app.js 的 api / esc / toast / drawPager。 */
let orderPage = 1, orderTotal = 0, orderRows = [], orderStatus = '';
let orderTabsBound = false;

const ORDER_COUNT_IDS = {
  '全部': 'orderCountAll', '待付款': 'orderCountPendingPay', '待发货': 'orderCountPendingShip',
  '已发货': 'orderCountShipped', '已完成': 'orderCountDone', '已取消': 'orderCountCancelled'
};

function resetOrderState() {
  orderPage = 1; orderTotal = 0; orderRows = []; orderStatus = '';
  const tbody = $('orderTbody');
  if (tbody) tbody.innerHTML = '';
  const pager = $('orderPager');
  if (pager) pager.innerHTML = '';
  document.querySelectorAll('#orderStatusTabs .order-tab').forEach((tab) =>
    tab.classList.toggle('active', !tab.dataset.status));
}

function initOrderView() {
  if (!orderTabsBound) {
    $('orderStatusTabs').addEventListener('click', (event) => {
      const tab = event.target.closest('.order-tab');
      if (tab) selectOrderStatus(tab.dataset.status || '');
    });
    ['oInpOrderId', 'oInpUserId', 'oInpProductName'].forEach((id) => {
      $(id).addEventListener('keydown', (event) => { if (event.key === 'Enter') searchOrders(); });
    });
    orderTabsBound = true;
  }
  loadOrderStats();
  loadOrders(orderPage);
}

async function loadOrderStats() {
  try {
    const res = await api('/Order/stats');
    const stats = res.data || {};
    Object.keys(ORDER_COUNT_IDS).forEach((name) => {
      const node = $(ORDER_COUNT_IDS[name]);
      if (node) node.textContent = Number(stats[name] || 0);
    });
  } catch (e) { notifyError(e); }
}

async function loadOrders(page) {
  orderPage = page || 1;
  try {
    const res = await api('/Order/list', 'GET', {
      page: orderPage, limit: PAGE_SIZE, status: orderStatus,
      orderId: $('oInpOrderId').value.trim(), userId: $('oInpUserId').value.trim(),
      productName: $('oInpProductName').value.trim(), channel: $('oSelChannel').value,
      startDate: $('oStartDate').value, endDate: $('oEndDate').value
    });
    orderRows = res.data || [];
    orderTotal = res.total == null ? orderRows.length : Number(res.total);
    renderOrderTable();
    drawPager('orderPager', orderPage, orderTotal, (next) => loadOrders(next));
  } catch (e) { notifyError(e); }
}

function selectOrderStatus(status) {
  orderStatus = status;
  document.querySelectorAll('#orderStatusTabs .order-tab').forEach((tab) =>
    tab.classList.toggle('active', (tab.dataset.status || '') === status));
  loadOrders(1);
}

function searchOrders() {
  const start = $('oStartDate').value, end = $('oEndDate').value;
  if (start && end && start > end) { toast('开始日期不能晚于结束日期', false); return; }
  loadOrders(1);
}

function resetOrderSearch() {
  ['oInpOrderId', 'oInpUserId', 'oInpProductName', 'oStartDate', 'oEndDate'].forEach((id) => { $(id).value = ''; });
  $('oSelChannel').value = '';
  selectOrderStatus('');
}

function orderStatusClass(status) {
  if (status === '待付款') return 'status-pending';
  if (status === '待发货' || status === '已发货') return 'status-active';
  if (status === '已完成') return 'status-done';
  return 'status-cancelled';
}

function orderActions(row, index) {
  let html = '<button type="button" class="btn" onclick="openOrderDetail(' + index + ')">详情</button>';
  if (row.orderStatus === '待付款') {
    html += '<button type="button" class="btn btn-primary" onclick="updateOrderStatus(' + index + ',\'待发货\')">确认付款</button>' +
      '<button type="button" class="btn btn-danger" onclick="updateOrderStatus(' + index + ',\'已取消\')">取消</button>';
  } else if (row.orderStatus === '待发货') {
    html += '<button type="button" class="btn btn-primary" onclick="updateOrderStatus(' + index + ',\'已发货\')">发货</button>' +
      '<button type="button" class="btn btn-danger" onclick="updateOrderStatus(' + index + ',\'已取消\')">取消</button>';
  } else if (row.orderStatus === '已发货') {
    html += '<button type="button" class="btn btn-primary" onclick="updateOrderStatus(' + index + ',\'已完成\')">完成</button>';
  } else if (row.orderStatus === '已完成' && Number(row.isReturn) !== 1) {
    html += '<button type="button" class="btn" onclick="processOrderAfterSale(' + index + ')">办理售后</button>';
  }
  return html;
}

async function processOrderAfterSale(index) {
  const row = orderRows[index];
  if (!row) return;
  const type = window.prompt('请输入售后类型：退款、退货退款或换货', '退货退款');
  if (!type) return;
  if (!['退款', '退货退款', '换货'].includes(type)) { toast('售后类型只能是退款、退货退款或换货', false); return; }
  const reason = window.prompt('请输入售后原因', '商品与预期不符');
  if (reason == null) return;
  let amount = '0';
  if (type !== '换货') {
    amount = window.prompt('请输入退款金额，不能超过实付金额 ' + Number(row.payAmount || 0).toFixed(2), Number(row.payAmount || 0).toFixed(2));
    if (amount == null) return;
  }
  if (!window.confirm('确定为订单 ' + row.orderId + ' 办理“' + type + '”吗？')) return;
  try {
    await api('/Order/afterSale', 'POST', { orderId: row.orderId, type: type, reason: reason, amount: amount });
    toast('售后处理完成');
    await Promise.all([loadOrders(orderPage), loadOrderStats()]);
  } catch (e) { notifyError(e); }
}

function renderOrderTable() {
  const tbody = $('orderTbody');
  if (!orderRows.length) {
    tbody.innerHTML = '<tr><td colspan="9" class="empty">没有符合条件的订单</td></tr>';
    return;
  }
  tbody.innerHTML = orderRows.map((row, index) => '<tr>' +
    '<td><span class="order-id">' + esc(row.orderId) + '</span></td>' +
    '<td><div class="order-product"><strong>' + esc(row.productName) + '</strong><span class="order-sub">' +
      esc(row.productId) + ' · ' + esc(row.category) + ' · ' + esc(row.quantity) + ' 件</span></div></td>' +
    '<td>' + esc(row.userId) + '</td>' +
    '<td><span class="order-money">¥ ' + Number(row.payAmount || 0).toFixed(2) + '</span>' +
      (Number(row.couponAmount || 0) ? '<div class="order-sub">优惠 ¥' + Number(row.couponAmount).toFixed(2) + '</div>' : '') + '</td>' +
    '<td>' + esc(row.orderChannel) + '</td><td>' + esc(row.orderTime || '—') + '</td>' +
    '<td>' + (Number(row.isReturn) === 1 ? '<span class="status-badge status-pending">' + esc(row.returnType || '售后中') + '</span>' : '—') + '</td>' +
    '<td><span class="status-badge ' + orderStatusClass(row.orderStatus) + '">' + esc(row.orderStatus) + '</span></td>' +
    '<td><div class="order-op">' + orderActions(row, index) + '</div></td></tr>').join('');
}

async function updateOrderStatus(index, targetStatus) {
  const row = orderRows[index];
  if (!row) return;
  const verb = targetStatus === '已取消' ? '取消' : (targetStatus === '已发货' ? '标记为已发货' : (targetStatus === '已完成' ? '完成' : '确认付款'));
  if (!window.confirm('确定要' + verb + '订单 ' + row.orderId + ' 吗？')) return;
  try {
    await api('/Order/updateStatus', 'POST', { orderId: row.orderId, status: targetStatus });
    toast('订单状态已更新为“' + targetStatus + '”');
    await Promise.all([loadOrders(orderPage), loadOrderStats()]);
  } catch (e) { notifyError(e); }
}

function orderDetailItem(label, value) {
  return '<div class="order-detail-item"><span class="order-detail-label">' + esc(label) + '</span>' +
    '<div class="order-detail-value">' + esc(value == null || value === '' ? '—' : value) + '</div></div>';
}

function orderTimeNode(name, value) {
  return '<div class="order-time-node' + (value ? ' done' : '') + '"><span class="order-time-name">' + esc(name) +
    '</span><span>' + esc(value || '—') + '</span></div>';
}

function openOrderDetail(index) {
  const row = orderRows[index];
  if (!row) return;
  $('orderDetailBody').innerHTML =
    '<div class="order-detail-head"><div><div class="order-detail-id">' + esc(row.orderId) + '</div><div class="order-sub">' +
      esc(row.productName) + ' · ' + esc(row.quantity) + ' 件</div></div><span class="status-badge ' + orderStatusClass(row.orderStatus) + '">' + esc(row.orderStatus) + '</span></div>' +
    '<div class="order-detail-grid">' +
      orderDetailItem('用户编号', row.userId) + orderDetailItem('商品编号', row.productId) + orderDetailItem('商品分类', row.category) +
      orderDetailItem('订单渠道', row.orderChannel) + orderDetailItem('实付金额', '¥ ' + Number(row.payAmount || 0).toFixed(2)) +
      orderDetailItem('优惠金额', '¥ ' + Number(row.couponAmount || 0).toFixed(2)) +
    '</div><div class="order-section-title">订单进度</div><div class="order-timeline">' +
      orderTimeNode('提交订单', row.orderTime) + orderTimeNode('支付', row.payTime) +
      orderTimeNode('发货', row.deliveryTime) + orderTimeNode('完成', row.completeTime) + '</div>' +
    '<div class="order-section-title">售后信息</div><div class="order-detail-grid">' +
      orderDetailItem('是否售后', Number(row.isReturn) === 1 ? '是' : '否') + orderDetailItem('售后类型', row.returnType) +
      orderDetailItem('售后原因', row.returnReason) + orderDetailItem('退款金额', row.returnAmount == null ? null : '¥ ' + Number(row.returnAmount).toFixed(2)) +
      orderDetailItem('售后时间', row.returnTime) + orderDetailItem('备注', row.remark) + '</div>';
  openModal('orderDetailModal');
}
