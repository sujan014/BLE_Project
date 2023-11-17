/*
 * Copyright 2021 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.VerticalAlignment
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.String


class CreateNewPdf {

    companion object {
        // LINE SEPARATOR
        val thick_ls = LineSeparator(SolidLine(2f))
        val thin_ls = LineSeparator(SolidLine(1f))
        val tempcolor = DeviceRgb(255, 192, 192)
        val humidcolor = DeviceRgb(0, 172, 192)

        fun newPdf(context: Context, uri: Uri, dataframe: MutableList<DataFrame>, testinfo: TestInfo, bitmap: Bitmap) {

            val mintemp = dataframe.minBy { it.temperature }?.temperature
            val maxtemp = dataframe.maxBy { it.temperature }?.temperature
            val avgtemp = (dataframe.map { it.temperature }.average() * 10f).toInt().toFloat() / 10f
            val minhumid = dataframe.minBy { it.humidity }?.humidity
            val maxhumid = dataframe.maxBy { it.humidity }?.humidity
            val avghumid = (dataframe.map { it.humidity }.average() * 10f).toInt().toFloat() / 10f

            val filename = queryName(context.contentResolver, uri)

            try {

                var writeStream: OutputStream? = context.contentResolver.openOutputStream(uri, "w")
                val pdf = PdfDocument(PdfWriter(writeStream))
                val document = Document(pdf, PageSize.A4, false)

                val reportTitle = Paragraph("DATA LOGGER REPORT").setFontSize(20f)
                    .setTextAlignment(TextAlignment.CENTER)
                document.add(reportTitle)
                document.add(thick_ls)

                val info_font = 12f
                val summary_font = 12f
                val data_font = 8f
                val page_font = 6f

                var header = Paragraph("1. Log Info")
                document.add(header)

                /*header = Paragraph("Device name: ${testinfo.devicename}").setFontSize(info_font)
                document.add(header)
                header = Paragraph("Log start date: ${testinfo.startDate}").setFontSize(info_font)
                document.add(header)
                header = Paragraph("Log end date: ${testinfo.endDate}").setFontSize(info_font)
                document.add(header)
                header = Paragraph("Report creation date: ${testinfo.currentdate}\n\n").setFontSize(info_font)
                document.add(header)*/

                var cell11: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Device name"))
                var cell12: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("${testinfo.devicename}"))
                var cell21: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Log start"))
                var cell22: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("${testinfo.startDate}"))
                var cell31 = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Log end"))
                var cell32: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("${testinfo.endDate}"))
                var cell41 = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Report date"))
                var cell42: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("${testinfo.currentdate}"))

                val infoTable = Table(2,false).setHorizontalAlignment(HorizontalAlignment.CENTER)
                infoTable.addCell(cell11)
                infoTable.addCell(cell12)
                infoTable.addCell(cell21)
                infoTable.addCell(cell22)
                infoTable.addCell(cell31)
                infoTable.addCell(cell32)
                infoTable.addCell(cell41)
                infoTable.addCell(cell42)
                document.add(infoTable)

                header = Paragraph("2. Summary")
                document.add(header)

                val table = Table(4, false)
                table.setHorizontalAlignment(HorizontalAlignment.CENTER)

                cell11 = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("List").setFontSize(summary_font))
                cell12 = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Maximum").setFontSize(summary_font))
                var cell13: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Minimum").setFontSize(summary_font))
                var cell14: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Average").setFontSize(summary_font))

                cell21 = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Temperature °C").setFontSize(summary_font))
                cell22 = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph(maxtemp.toString()).setFontSize(summary_font))
                val cell23: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph(mintemp.toString()).setFontSize(summary_font))
                val cell24: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph(avgtemp.toString()).setFontSize(summary_font))

                cell31 = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER).add(Paragraph("Humidity %"))
                cell32 = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph(maxhumid.toString()).setFontSize(summary_font))
                val cell33: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph(minhumid.toString()).setFontSize(summary_font))
                val cell34: Cell = Cell(1, 1).setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph(avghumid.toString()).setFontSize(summary_font))

                table.addCell(cell11)
                table.addCell(cell12)
                table.addCell(cell13)
                table.addCell(cell14)
                table.addCell(cell21)
                table.addCell(cell22)
                table.addCell(cell23)
                table.addCell(cell24)
                table.addCell(cell31)
                table.addCell(cell32)
                table.addCell(cell33)
                table.addCell(cell34)
                document.add(table)

                header = Paragraph("3. Real time chart data").setFontSize(12f)
                document.add(header)

                var stream: ByteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                //val byteArray: ByteArray = stream.toByteArray()
                var img: Image = Image(ImageDataFactory.create(stream.toByteArray()))
                document.add(img)

                header = Paragraph("4. Log Details").setFontSize(12f)
                document.add(header)
                var dTable = Table(4, false)
                var celld1 = Cell()
                var celld2 = Cell()
                var celld3 = Cell()
                var celld4 = Cell()
                var row = 0
                cell11 = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER).add(Paragraph("List").setFontSize(data_font))
                cell12 = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Date Time").setFontSize(data_font))
                cell13 = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Temperature").setFontSize(data_font))
                cell14 = Cell(1, 1).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(Paragraph("Humidity").setFontSize(data_font))
                dTable.addCell(cell11)
                dTable.addCell(cell12)
                dTable.addCell(cell13)
                dTable.addCell(cell14)
                dataframe.forEach {
                    row++
                    celld1 = Cell(row, 1).setBackgroundColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER).
                                add(Paragraph(it.count.toString()).setFontSize(data_font)).setWidth(30f)
                    celld2 = Cell(row, 1).setBackgroundColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER).
                                add(Paragraph(it.datetime).
                                setFontSize(data_font)).setWidth(85f)
                    celld3 = Cell(row, 1).setBackgroundColor(tempcolor).setTextAlignment(TextAlignment.CENTER).
                                add(Paragraph("${it.temperature.toString()} °C")
                                    .setFontSize(data_font)).setWidth(60f)
                    celld4 = Cell(row, 1).setBackgroundColor(humidcolor).setTextAlignment(TextAlignment.CENTER).
                                add(Paragraph("${it.humidity.toString()} %")
                                    .setFontSize(data_font)).setWidth(60f)

                    dTable.addCell(celld1)
                    dTable.addCell(celld2)
                    dTable.addCell(celld3)
                    dTable.addCell(celld4)
                }
                document.add(dTable)

                val n: Int = pdf.numberOfPages
                for (i in 1..n) {
                    document.showTextAligned(
                        Paragraph(String.format("Page $i of $n")).setFontSize(page_font),
                        559f, 836f, i,
                        TextAlignment.RIGHT,
                        VerticalAlignment.TOP,
                        0f
                    )
                }
                document.flush()

                document.close()
            } catch (e: Exception){
                Toast.makeText(context, "Error when $filename.pdf", Toast.LENGTH_SHORT).show()
            }
        }

        private fun queryName(resolver: ContentResolver, uri: Uri): kotlin.String {
            val returnCursor: Cursor = resolver.query(uri, null, null, null, null)!!
            val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            val name: kotlin.String = returnCursor.getString(nameIndex)
            returnCursor.close()
            return name
        }
    }
}