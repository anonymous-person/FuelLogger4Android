/**
 * FuelLogger - Google Apps Script Backend
 * 
 * SETUP:
 * 1. Open your Google Sheet
 * 2. Extensions → Apps Script → paste this entire file
 * 3. Save, then Deploy → New Deployment → Web App
 * 4. Execute as: Me | Who has access: Anyone
 * 5. Click Deploy, authorize, copy the URL → paste into FuelLogger app Settings
 */

const SHEET_NAME = "Fuel Log";

const COLUMNS = [
  "Timestamp",
  "Date",
  "Time",
  "Station",
  "Address",
  "City",
  "State",
  "Pump #",
  "Fuel Grade",
  "Gallons",
  "Price/Gal ($)",
  "Total ($)",
  "Odometer (mi)",
  "Trip Miles",
  "Temp (°F)",
  "Payment",
  "Card Last 4",
  "Invoice #",
  "Auth Code"
];

function getOrCreateSheet() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_NAME);
  
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_NAME);
    setupHeaders(sheet);
  } else if (sheet.getLastRow() === 0) {
    setupHeaders(sheet);
  }
  
  return sheet;
}

function setupHeaders(sheet) {
  const headerRange = sheet.getRange(1, 1, 1, COLUMNS.length);
  headerRange.setValues([COLUMNS]);
  
  // Style headers
  headerRange.setBackground("#1B5E20");
  headerRange.setFontColor("#FFFFFF");
  headerRange.setFontWeight("bold");
  headerRange.setFontSize(11);
  
  // Freeze header row
  sheet.setFrozenRows(1);
  
  // Set column widths
  const widths = [160, 100, 90, 160, 200, 120, 60, 70, 90, 80, 100, 80, 110, 90, 80, 90, 90, 100, 90];
  widths.forEach((w, i) => sheet.setColumnWidth(i + 1, w));
  
  // Add auto-filter
  sheet.getRange(1, 1, 1, COLUMNS.length).createFilter();
  
  // Number formats
  sheet.getRange(2, 10, 1000, 1).setNumberFormat("0.000");   // Gallons
  sheet.getRange(2, 11, 1000, 1).setNumberFormat("$0.000");  // Price/gal
  sheet.getRange(2, 12, 1000, 1).setNumberFormat("$#,##0.00"); // Total
  sheet.getRange(2, 13, 1000, 1).setNumberFormat("#,##0");   // Odometer
}

function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);
    const sheet = getOrCreateSheet();
    
    const row = [
      new Date(),                    // Timestamp (auto)
      data.date || "",
      data.time || "",
      data.station || "",
      data.address || "",
      data.city || "",
      data.state || "",
      data.pumpNumber || "",
      data.fuelGrade || "",
      parseFloat(data.gallons) || 0,
      parseFloat(data.pricePerGallon) || 0,
      parseFloat(data.totalAmount) || 0,
      parseInt(data.odometer) || 0,
      parseFloat(data.tripMiles) || 0,
      parseInt(data.temperature) || 0,
      data.paymentMethod || "",
      data.cardLast4 || "",
      data.invoiceNumber || "",
      data.authCode || ""
    ];
    
    sheet.appendRow(row);
    
    // Alternate row shading for readability
    const lastRow = sheet.getLastRow();
    if (lastRow % 2 === 0) {
      sheet.getRange(lastRow, 1, 1, COLUMNS.length).setBackground("#F1F8E9");
    }
    
    // Update summary sheet
    updateSummary();
    
    return ContentService
      .createTextOutput(JSON.stringify({ success: true, row: lastRow }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ success: false, error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  try {
    const action = e.parameter.action;
    
    if (action === "get") {
      return getEntries();
    }
    
    // Default: return status
    return ContentService
      .createTextOutput(JSON.stringify({ status: "FuelLogger API running" }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function getEntries() {
  const sheet = getOrCreateSheet();
  const lastRow = sheet.getLastRow();
  
  if (lastRow <= 1) {
    return ContentService
      .createTextOutput("[]")
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  const data = sheet.getRange(2, 1, lastRow - 1, COLUMNS.length).getValues();
  
  const entries = data.map(row => ({
    timestamp:      row[0] ? row[0].toString() : "",
    date:           row[1],
    time:           row[2],
    station:        row[3],
    address:        row[4],
    city:           row[5],
    state:          row[6],
    pumpNumber:     row[7],
    fuelGrade:      row[8],
    gallons:        row[9],
    pricePerGallon: row[10],
    totalAmount:    row[11],
    odometer:       row[12],
    tripMiles:      row[13],
    temperature:    row[14],
    paymentMethod:  row[15],
    cardLast4:      row[16],
    invoiceNumber:  row[17],
    authCode:       row[18]
  }));
  
  return ContentService
    .createTextOutput(JSON.stringify(entries))
    .setMimeType(ContentService.MimeType.JSON);
}

function updateSummary() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let summary = ss.getSheetByName("Summary");
  
  if (!summary) {
    summary = ss.insertSheet("Summary");
  }
  
  summary.clearContents();
  
  const fuelSheet = getOrCreateSheet();
  const lastRow = fuelSheet.getLastRow();
  
  if (lastRow <= 1) return;
  
  // Summary title
  summary.getRange("A1").setValue("⛽ FuelLogger Summary");
  summary.getRange("A1").setFontSize(16).setFontWeight("bold").setFontColor("#1B5E20");
  summary.getRange("A2").setValue("Last updated: " + new Date().toLocaleString());
  summary.getRange("A2").setFontColor("#757575");
  
  const dataRange = `'${SHEET_NAME}'!A2:M${lastRow}`;
  
  const stats = [
    ["", ""],
    ["LIFETIME STATS", ""],
    ["Total Fill-Ups", `=COUNTA('${SHEET_NAME}'!B2:B${lastRow})`],
    ["Total Gallons", `=SUM('${SHEET_NAME}'!J2:J${lastRow})`],
    ["Total Spent ($)", `=SUM('${SHEET_NAME}'!L2:L${lastRow})`],
    ["Avg Price/Gal ($)", `=AVERAGE('${SHEET_NAME}'!K2:K${lastRow})`],
    ["Avg Fill-Up ($)", `=AVERAGE('${SHEET_NAME}'!L2:L${lastRow})`],
    ["Avg Gallons/Fill", `=AVERAGE('${SHEET_NAME}'!J2:J${lastRow})`],
    ["Last Odometer", `=MAX('${SHEET_NAME}'!M2:M${lastRow})`],
    ["", ""],
    ["LAST FILL-UP", ""],
    ["Date", `='${SHEET_NAME}'!B${lastRow}`],
    ["Station", `='${SHEET_NAME}'!D${lastRow}`],
    ["Total ($)", `='${SHEET_NAME}'!L${lastRow}`],
    ["Price/Gal ($)", `='${SHEET_NAME}'!K${lastRow}`],
    ["Gallons", `='${SHEET_NAME}'!J${lastRow}`],
    ["Odometer", `='${SHEET_NAME}'!M${lastRow}`],
  ];
  
  summary.getRange(3, 1, stats.length, 2).setValues(stats);
  
  // Style headers
  summary.getRange("A4").setFontWeight("bold").setFontColor("#1B5E20");
  summary.getRange("A13").setFontWeight("bold").setFontColor("#1B5E20");
  
  // Format numbers
  summary.getRange("B7").setNumberFormat("$#,##0.00");
  summary.getRange("B8").setNumberFormat("$0.000");
  summary.getRange("B9").setNumberFormat("$#,##0.00");
  summary.getRange("B15").setNumberFormat("$#,##0.00");
  summary.getRange("B16").setNumberFormat("$0.000");
  
  summary.setColumnWidth(1, 160);
  summary.setColumnWidth(2, 160);
}
