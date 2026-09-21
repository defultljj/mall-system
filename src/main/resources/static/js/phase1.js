/* 阶段 1：规格参数 / 销售属性独立维护页。 */
VIEW_TITLES.specAttr = '规格参数管理';
VIEW_TITLES.saleAttrManage = '销售属性管理';
MODAL_IDS.push('attrManageModal');

const managedAttrState = {
  1: { prefix: 'specAttr', categoryId: null, page: 1, total: 0, rows: [] },
  2: { prefix: 'saleAttrManage', categoryId: null, page: 1, total: 0, rows: [] }
};

function managedAttrLabel(type) { return Number(type) === 1 ? '规格参数' : '销售属性'; }
function isLeafCategory(node) {
  return !!node && Number(node.level) === 3 &&
    (!Array.isArray(node.children) || node.children.length === 0);
}

async function initPhase1AttrView(type) {
  try {
    await ensureCat();
    renderAttrCategoryTree(type);
    const state = managedAttrState[type];
    if (state.categoryId && catCache.byId[state.categoryId]) {
      await loadManagedAttrGroups(type);
      await loadManagedAttrs(type, state.page);
    } else renderManagedAttrTable(type);
  } catch (e) { notifyError(e); }
}

function renderAttrCategoryTree(type) {
  const state = managedAttrState[type];
  const prefix = state.prefix;
  const box = $(prefix + 'CatTree');
  if (!box) return;
  const keyword = ($(prefix + 'CatSearch').value || '').trim().toLowerCase();
  let nodes = (catCache.flat || []).map((x) => catCache.byId[x.id]).filter(Boolean);
  if (keyword) nodes = nodes.filter((n) => String(n.categoryName || '').toLowerCase().includes(keyword));
  if (!nodes.length) { box.innerHTML = '<div class="empty attr-tree-empty">未找到匹配类别</div>'; return; }
  box.innerHTML = nodes.map((n) => {
    const leaf = isLeafCategory(n);
    const active = Number(state.categoryId) === Number(n.id);
    const level = Number(n.level || 1);
    return '<button type="button" class="attr-cat-item' + (active ? ' active' : '') +
      (leaf ? '' : ' branch') + '" style="padding-left:' + (12 + (level - 1) * 16) + 'px"' +
      (leaf ? ' onclick="selectManagedAttrCategory(' + type + ',' + n.id + ')"' : ' disabled') + '>' +
      '<span class="attr-cat-mark">' + (leaf ? '•' : '›') + '</span>' + esc(n.categoryName) +
      '<small>L' + level + '</small></button>';
  }).join('');
}

async function selectManagedAttrCategory(type, categoryId) {
  const node = catCache.byId[categoryId];
  if (!isLeafCategory(node)) { toast('请选择第三级叶子类别', false); return; }
  const state = managedAttrState[type];
  state.categoryId = Number(categoryId); state.page = 1;
  $(state.prefix + 'Add').disabled = false;
  $(state.prefix + 'Context').textContent = catPathLabel(node) + ' / ' + (node.categoryName || '');
  renderAttrCategoryTree(type);
  await loadManagedAttrGroups(type);
  await loadManagedAttrs(type, 1);
}

async function loadManagedAttrGroups(type) {
  const state = managedAttrState[type];
  const select = $(state.prefix + 'Group');
  const oldValue = select.value;
  if (!state.categoryId) { select.innerHTML = '<option value="">全部分组</option>'; return []; }
  const res = await api('/AttrGroup/optionsByCategory', 'GET', { categoryId: state.categoryId });
  const options = res.data || [];
  select.innerHTML = '<option value="">全部分组</option>' + options.map((o) =>
    '<option value="' + esc(o.value) + '">' + esc(o.label) + '</option>').join('');
  select.value = oldValue;
  return options;
}

async function loadManagedAttrs(type, page) {
  const state = managedAttrState[type];
  if (!state.categoryId) { renderManagedAttrTable(type); return; }
  try {
    state.page = page || 1;
    const res = await api('/GoodsAttr/list', 'GET', {
      page: state.page, limit: PAGE_SIZE, categoryId: state.categoryId, attrType: type,
      attrName: $(state.prefix + 'Name').value.trim(), attrGroupId: $(state.prefix + 'Group').value
    });
    state.rows = res.data || [];
    state.total = Number(res.total == null ? state.rows.length : res.total);
    renderManagedAttrTable(type);
    drawPager(state.prefix + 'Pager', state.page, state.total, (p) => loadManagedAttrs(type, p));
  } catch (e) { notifyError(e); }
}

function renderManagedAttrTable(type) {
  const state = managedAttrState[type];
  const tbody = $(state.prefix + 'Tbody');
  const pager = $(state.prefix + 'Pager');
  if (!state.categoryId) {
    tbody.innerHTML = '<tr><td colspan="8" class="empty">请先从左侧选择末级类别</td></tr>';
    pager.innerHTML = ''; return;
  }
  if (!state.rows.length) {
    tbody.innerHTML = '<tr><td colspan="8" class="empty">当前类别暂无' + managedAttrLabel(type) + '</td></tr>';
    return;
  }
  tbody.innerHTML = state.rows.map((r, i) => '<tr>' +
    '<td>' + esc(r.id) + '</td><td>' + esc(r.attrName) + '</td><td>' + esc(r.groupName || '未分组') + '</td>' +
    '<td>' + (Number(r.valueType) === 2 ? '多值' : '单值') + '</td>' +
    '<td class="attr-values" title="' + esc(r.attrValue || '') + '">' + esc(r.attrValue || '—') + '</td>' +
    '<td>' + statusDot(r.searchEnable) + (Number(r.searchEnable) === 1 ? '是' : '否') + '</td>' +
    '<td>' + statusDot(r.enable) + (Number(r.enable) === 1 ? '启用' : '停用') + '</td>' +
    '<td class="op"><button type="button" class="link-btn" onclick="openAttrManageModal(' + type + ',' + i + ')">编辑</button>' +
    '<button type="button" class="link-btn danger" onclick="deleteManagedAttr(' + type + ',' + i + ')">删除</button></td></tr>').join('');
}

async function openAttrManageModal(type, rowIndex) {
  const state = managedAttrState[type];
  const node = catCache.byId[state.categoryId];
  if (!node) { toast('请先选择末级类别', false); return; }
  const row = rowIndex == null ? null : state.rows[rowIndex];
  $('attrManageModalTitle').textContent = (row ? '编辑' : '新增') + managedAttrLabel(type);
  $('amId').value = row ? row.id : ''; $('amType').value = type;
  $('amCategoryLabel').textContent = catPathLabel(node) + ' / ' + node.categoryName;
  $('amName').value = row ? (row.attrName || '') : '';
  $('amValueType').value = row ? String(row.valueType || 1) : (Number(type) === 2 ? '2' : '1');
  $('amValues').value = row ? (row.attrValue || '') : '';
  $('amSearchEnable').checked = row ? Number(row.searchEnable) === 1 : false;
  $('amEnable').checked = row ? Number(row.enable) === 1 : true;
  try {
    const res = await api('/AttrGroup/optionsByCategory', 'GET', { categoryId: state.categoryId });
    $('amGroup').innerHTML = '<option value="">不分组</option>' + (res.data || []).map((o) =>
      '<option value="' + esc(o.value) + '">' + esc(o.label) + '</option>').join('');
    $('amGroup').value = row && row.attrGroupId != null ? String(row.attrGroupId) : '';
    openModal('attrManageModal');
  } catch (e) { notifyError(e); }
}

function normalizedAttrValues(value) {
  const seen = new Set();
  return String(value || '').split(/[;；,，\n]+/).map((v) => v.trim()).filter((v) => {
    if (!v || seen.has(v)) return false;
    seen.add(v); return true;
  }).join(';');
}

async function saveManagedAttr() {
  const type = Number($('amType').value);
  const state = managedAttrState[type];
  const id = $('amId').value;
  const attrName = $('amName').value.trim();
  if (!attrName) { toast('属性名称不能为空', false); return; }
  try {
    await api(id ? '/GoodsAttr/update' : '/GoodsAttr/save', id ? 'PUT' : 'POST', {
      id: id || undefined, attrName, categoryId: state.categoryId, attrType: type,
      valueType: $('amValueType').value, attrValue: normalizedAttrValues($('amValues').value),
      attrGroupId: $('amGroup').value, searchEnable: $('amSearchEnable').checked ? 1 : 0,
      enable: $('amEnable').checked ? 1 : 0
    });
    closeModal('attrManageModal'); toast(id ? '属性修改成功' : '属性新增成功');
    await loadManagedAttrs(type, state.page);
  } catch (e) { notifyError(e); }
}

async function deleteManagedAttr(type, rowIndex) {
  const state = managedAttrState[type]; const row = state.rows[rowIndex];
  if (!row || !window.confirm('确定删除「' + row.attrName + '」吗？已被商品使用的属性不能删除。')) return;
  try {
    await api('/GoodsAttr/delete', 'DELETE', { id: row.id }); toast('删除成功');
    if (state.rows.length === 1 && state.page > 1) state.page--;
    await loadManagedAttrs(type, state.page);
  } catch (e) { notifyError(e); }
}

function resetManagedAttrFilters(type) {
  const state = managedAttrState[type];
  $(state.prefix + 'Name').value = ''; $(state.prefix + 'Group').value = '';
  loadManagedAttrs(type, 1);
}

function resetPhase1Views() {
  [1, 2].forEach((type) => {
    const state = managedAttrState[type];
    state.categoryId = null; state.page = 1; state.total = 0; state.rows = [];
    ['CatSearch', 'Name'].forEach((suffix) => { const el = $(state.prefix + suffix); if (el) el.value = ''; });
    const add = $(state.prefix + 'Add'); if (add) add.disabled = true;
    const tbody = $(state.prefix + 'Tbody'); if (tbody) tbody.innerHTML = '';
    const pager = $(state.prefix + 'Pager'); if (pager) pager.innerHTML = '';
  });
}
