const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const fields = new Map();
const sandbox = {
  console,
  document: {
    getElementById: (id) => fields.get(id) || null,
    addEventListener: () => {},
    querySelectorAll: () => []
  },
  window: { localStorage: { getItem: () => null, setItem: () => {} } },
  FormData: class {},
  URLSearchParams,
  fetch: async () => { throw new Error('unexpected fetch'); },
  setTimeout,
  clearTimeout
};

const appPath = path.resolve(__dirname, '../../main/resources/static/js/app.js');
const source = fs.readFileSync(appPath, 'utf8');
vm.runInNewContext(source + `
  saleAttrForm.rows = [{ id: 1104, attrName: '颜色', valueType: 2, valueArr: ['黑色'] }];
  globalThis.__saleAttrForm = saleAttrForm;
  globalThis.__buildAttrFormList = buildAttrFormList;
`, sandbox);

fields.set('saleAttrTagInp0', { value: '白色' });
const result = sandbox.__buildAttrFormList(sandbox.__saleAttrForm);
assert.deepEqual(JSON.parse(JSON.stringify(result)), [
  { attrId: 1104, valueType: 2, attrValue: '黑色;白色' }
]);
console.log('SKU state regression check passed.');
