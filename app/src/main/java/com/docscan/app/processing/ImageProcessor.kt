package com.docscan.app.processing

import android.content.Context
import android.graphics.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import com.docscan.app.model.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageProcessor {

    init {
        OpenCVLoader.initDebug()
    }

    suspend fun applyFilter(
        bitmap: Bitmap,
        filterType: FilterType
    ): Bitmap = withContext(Dispatchers.Default) {
        when (filterType) {
            FilterType.ORIGINAL -> bitmap
            FilterType.GRAYSCALE -> toGrayscale(bitmap)
            FilterType.BLACK_WHITE -> toBlackAndWhite(bitmap)
            FilterType.SHARPEN -> sharpen(bitmap)
            FilterType.AUTO_ENHANCE -> autoEnhance(bitmap)
        }
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val src = Mat()
        val dst = Mat()
        Utils.bitmapToMat(bitmap, src)
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_GRAY2RGBA)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, result)
        src.release()
        dst.release()
        return result
    }

    private fun toBlackAndWhite(bitmap: Bitmap): Bitmap {
        val src = Mat()
        val gray = Mat()
        val dst = Mat()
        Utils.bitmapToMat(bitmap, src)
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.adaptiveThreshold(
            gray, dst, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY, 15, 10.0
        )
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_GRAY2RGBA)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, result)
        src.release()
        gray.release()
        dst.release()
        return result
    }

    private fun sharpen(bitmap: Bitmap): Bitmap {
        val src = Mat()
        val dst = Mat()
        Utils.bitmapToMat(bitmap, src)
        val kernel = Mat(3, 3, CvType.CV_32FC1)
        kernel.put(0, 0, floatArrayOf(0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f))
        Imgproc.filter2D(src, dst, -1, kernel)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, result)
        src.release()
        dst.release()
        kernel.release()
        return result
    }

    private fun autoEnhance(bitmap: Bitmap): Bitmap {
        val src = Mat()
        val dst = Mat()
        val lab = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Convert to LAB color space for contrast enhancement
        Imgproc.cvtColor(src, lab, Imgproc.COLOR_RGBA2RGB)
        val rgb = Mat()
        lab.copyTo(rgb)
        Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)

        val channels = ArrayList<Mat>(3)
        Core.split(lab, channels)

        // Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(channels[0], channels[0])

        Core.merge(channels, lab)
        Imgproc.cvtColor(lab, dst, Imgproc.COLOR_Lab2RGB)
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_RGB2RGBA)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, result)

        src.release()
        dst.release()
        lab.release()
        rgb.release()
        channels.forEach { it.release() }
        return result
    }

    fun detectDocumentEdges(bitmap: Bitmap): List<PointF> {
        val src = Mat()
        val gray = Mat()
        val blurred = Mat()
        val edges = Mat()
        val contours = mutableListOf<MatOfPoint>()

        try {
            Utils.bitmapToMat(bitmap, src)
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(blurred, edges, 75.0, 200.0)

            val hierarchy = Mat()
            Imgproc.findContours(
                edges, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
            )
            hierarchy.release()

            var maxArea = 0.0
            var bestContour: MatOfPoint2f? = null

            for (contour in contours) {
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

                if (approx.rows() == 4) {
                    val area = Imgproc.contourArea(approx)
                    if (area > maxArea && area > bitmap.width * bitmap.height * 0.1) {
                        maxArea = area
                        bestContour = approx
                    }
                }
                contour2f.release()
                if (approx != bestContour) approx.release()
            }

            if (bestContour != null) {
                val points = bestContour.toArray().map { PointF(it.x.toFloat(), it.y.toFloat()) }
                bestContour.release()
                return orderPoints(points)
            }

            // Default: return image corners
            return listOf(
                PointF(0f, 0f),
                PointF(bitmap.width.toFloat(), 0f),
                PointF(bitmap.width.toFloat(), bitmap.height.toFloat()),
                PointF(0f, bitmap.height.toFloat())
            )
        } finally {
            src.release()
            gray.release()
            blurred.release()
            edges.release()
            contours.forEach { it.release() }
        }
    }

    fun perspectiveTransform(bitmap: Bitmap, points: List<PointF>): Bitmap {
        if (points.size != 4) return bitmap

        val srcMat = Mat(4, 1, CvType.CV_32FC2)
        srcMat.put(0, 0, floatArrayOf(points[0].x, points[0].y))
        srcMat.put(1, 0, floatArrayOf(points[1].x, points[1].y))
        srcMat.put(2, 0, floatArrayOf(points[2].x, points[2].y))
        srcMat.put(3, 0, floatArrayOf(points[3].x, points[3].y))

        val widthTop = euclideanDistance(points[0], points[1])
        val widthBottom = euclideanDistance(points[3], points[2])
        val maxWidth = Math.max(widthTop, widthBottom)

        val heightLeft = euclideanDistance(points[0], points[3])
        val heightRight = euclideanDistance(points[1], points[2])
        val maxHeight = Math.max(heightLeft, heightRight)

        val dstMat = Mat(4, 1, CvType.CV_32FC2)
        dstMat.put(0, 0, floatArrayOf(0f, 0f))
        dstMat.put(1, 0, floatArrayOf(maxWidth, 0f))
        dstMat.put(2, 0, floatArrayOf(maxWidth, maxHeight))
        dstMat.put(3, 0, floatArrayOf(0f, maxHeight))

        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val dst = Mat()

        val perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        Imgproc.warpPerspective(src, dst, perspectiveTransform, Size(maxWidth.toDouble(), maxHeight.toDouble()))

        val result = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, result)

        src.release()
        dst.release()
        srcMat.release()
        dstMat.release()
        perspectiveTransform.release()

        return result
    }

    private fun orderPoints(points: List<PointF>): List<PointF> {
        val sorted = points.sortedWith(compareBy({ it.y }, { it.x }))
        val topPoints = sorted.take(2).sortedBy { it.x }
        val bottomPoints = sorted.takeLast(2).sortedByDescending { it.x }
        return listOf(topPoints[0], topPoints[1], bottomPoints[0], bottomPoints[1])
    }

    private fun euclideanDistance(a: PointF, b: PointF): Float {
        return Math.sqrt(((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)).toDouble()).toFloat()
    }

    suspend fun saveBitmap(context: Context, bitmap: Bitmap, name: String): String =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "scans/$name.jpg")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            file.absolutePath
        }
}