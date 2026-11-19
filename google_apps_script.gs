/**
 * Google Apps Script для двусторонней синхронизации «Брифи» v6.0 с Google Таблицей.
 *
 * Поддерживаемые действия:
 * - create: добавление новой задачи
 * - updateStatus: смена статуса («В работе», «Макет готов» и др.)
 * - updateNote: сохранение заметки к заказу
 * - saveLink: запись ссылки на макет в колонку I («Ссылка»)
 * - confirmPayout: обновление статуса оплаты текущей недели в колонке J
 * - delete: перевод заказа в статус «Отмена»
 */

const SHEET_NAME = 'Общая таблица';

function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const sheet = ss.getSheetByName(SHEET_NAME) || ss.getSheets()[0];
    const action = data.action || '';

    if (action === 'create') {
      const targetRow = findFirstEmptyOrderRow(sheet);
      const commentText = data.note
        ? (data.title + ' | Заметка: ' + data.note)
        : (data.title || '');
      sheet.getRange(targetRow, 1, 1, 8).setValues([[
        data.orderNumber || 'Новый',
        0,
        data.createdDate || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), 'dd.MM.yyyy'),
        data.status || 'В очереди',
        'Не оплачено',
        data.deadline || '',
        data.urgency || '!!',
        commentText
      ]]);
      return jsonResponse({ ok: true, row: targetRow });
    }

    if (action === 'confirmPayout') {
      const payoutRow = findLatestWeeklyPayoutStatusRow(sheet);
      if (payoutRow < 2) {
        return jsonResponse({ ok: false, error: 'Weekly payout block not found' });
      }
      sheet.getRange(payoutRow, 10).setValue(data.payoutStatus || 'Выплачено переводом');
      return jsonResponse({ ok: true, row: payoutRow });
    }

    const rowIdx = findOrderRow(sheet, data.rowNumber, data.orderNumber, data.title);
    if (rowIdx < 2) {
      return jsonResponse({ ok: false, error: 'Order row not found' });
    }

    if (action === 'updateStatus') {
      sheet.getRange(rowIdx, 4).setValue(data.status || 'В очереди');
      return jsonResponse({ ok: true, row: rowIdx });
    }

    if (action === 'updateNote') {
      const existing = String(sheet.getRange(rowIdx, 8).getValue() || '');
      const baseTitle = existing.split(' | Заметка:')[0].trim();
      const updated = data.note ? (baseTitle + ' | Заметка: ' + data.note) : baseTitle;
      sheet.getRange(rowIdx, 8).setValue(updated);
      return jsonResponse({ ok: true, row: rowIdx });
    }

    if (action === 'saveLink') {
      const url = String(data.link || '').trim();
      if (url) {
        sheet.getRange(rowIdx, 9).setValue(url);
      }
      return jsonResponse({ ok: true, row: rowIdx });
    }

    if (action === 'delete') {
      sheet.getRange(rowIdx, 4).setValue('Отмена');
      return jsonResponse({ ok: true, row: rowIdx });
    }

    return jsonResponse({ ok: false, error: 'Unknown action: ' + action });
  } catch (err) {
    return jsonResponse({ ok: false, error: String(err) });
  }
}

function findLatestWeeklyPayoutStatusRow(sheet) {
  const lastRow = sheet.getLastRow();
  if (lastRow < 2) return -1;
  const colJ = sheet.getRange(1, 10, lastRow, 1).getValues();
  let foundRow = -1;
  for (let i = 0; i < colJ.length; i++) {
    const val = String(colJ[i][0] || '').trim();
    if (val.toLowerCase().indexOf('итого за неделю') === 0) {
      foundRow = i + 2; // строка статуса оплаты идёт сразу под «Итого за неделю»
    }
  }
  return foundRow;
}

function findFirstEmptyOrderRow(sheet) {
  const lastRow = Math.max(sheet.getLastRow(), 2);
  const values = sheet.getRange(2, 1, lastRow - 1, 8).getValues();
  for (let i = 0; i < values.length; i++) {
    const orderNum = String(values[i][0] || '').trim();
    const comment = String(values[i][7] || '').trim();
    if (!orderNum && !comment) {
      return i + 2;
    }
  }
  return lastRow + 1;
}

function findOrderRow(sheet, rowNumber, orderNumber, title) {
  if (rowNumber && rowNumber >= 2) {
    const cellOrder = String(sheet.getRange(rowNumber, 1).getValue() || '').trim();
    const cellTitle = String(sheet.getRange(rowNumber, 8).getValue() || '').trim();
    if ((orderNumber && cellOrder === orderNumber) || (title && cellTitle.indexOf(title) === 0)) {
      return rowNumber;
    }
  }
  const lastRow = sheet.getLastRow();
  if (lastRow < 2) return -1;
  const values = sheet.getRange(2, 1, lastRow - 1, 8).getValues();
  for (let i = values.length - 1; i >= 0; i--) {
    const cOrder = String(values[i][0] || '').trim();
    const cTitle = String(values[i][7] || '').trim();
    if (orderNumber && orderNumber !== 'Без номера' && cOrder === orderNumber) {
      return i + 2;
    }
    if (title && cTitle && cTitle.indexOf(title) === 0) {
      return i + 2;
    }
  }
  return -1;
}

function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
