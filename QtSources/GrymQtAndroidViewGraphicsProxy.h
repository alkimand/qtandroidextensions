#pragma once
#include <QtOpenGL/QGLWidget>
#include <QtGui/QGraphicsWidget>
#include <QPaintEvent>
#include <EGL/egl.h>
#include "JNIUtils/JclassPtr.h"
#include "JNIUtils/jcGeneric.h"

class GrymQtAndroidViewGraphicsProxy
	: public QGraphicsWidget
{
	Q_OBJECT
public:
	GrymQtAndroidViewGraphicsProxy(QGraphicsItem *parent = 0, Qt::WindowFlags wFlags = 0);
	virtual ~GrymQtAndroidViewGraphicsProxy();

protected:
	virtual void paint(QPainter *painter, const QStyleOptionGraphicsItem *option, QWidget *widget);
	//! \param x, y, w, h - координаты региона в терминах OpenGL
	virtual void doGLPainting(int x, int y, int w, int h);
	void initTexture();
	void updateTexture(QPainter * painter);
	void destroyTexture();

	QSize getDrawableSize() const;

private:
	void CreateTestTexture(QSize * out_texture_size_);
	void CreateEmptyTexture(int desired_width, int desired_height, QSize * out_texture_size_);

private:
	GLuint texture_id_;
	GLenum texture_type_;
	bool texture_available_;
	QSize texture_size_;
	QScopedPointer<jcGeneric> offscreen_view_factory_;
	QScopedPointer<jcGeneric> offscreen_view_;
};
