/*
 * Copyright (c) 2013, INSA of Rennes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include "openhevc_sse.h"

#include "hevcpred.h"
#include "hevcdsp.h"
#include "config.h"

#if HAVE_SSE2
#include <emmintrin.h>
#endif
#if HAVE_SSE3
#include <tmmintrin.h>
#endif
#if HAVE_SSE4
#include <smmintrin.h>
#endif

static HEVCPredContext hevcPred;
static HEVCDSPContext hevcDsp;


static const int LUT_PEL_FUNC[65] = {
    -1, -1,  0, -1,  1, -1,  2, -1,
     3, -1, -1,  1,  4, -1, -1, -1,
     5, -1, -1, -1, -1, -1, -1, -1,
     6, -1, -1, -1, -1, -1, -1, -1,
     7, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1,
     8, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1,
     9};

/* Log2CbSize in openHEVC */
/* 1 -  3 -  5 -  7 - 11 - 15 - 23 - 31 - 47 - 63 --> _width
   2 -  4 -  6 -  8 - 12 - 16 - 24 - 32 - 48 - 64 --> width
   2 -  4 -  2 -  8 -  4 - 16 -  8 - 32 - 16 - 64 --> vector size
   0 -  1 -  0 -  2 -  1 -  3 -  2 -  4 -  3 -  5 --> function id
   */
static const int LUT_WEIGHTED_FUNC[65] = {
   -1, -1,  0, -1,  1, -1,  0, -1,
    2, -1, -1, -1,  1, -1, -1, -1,
    3, -1, -1, -1, -1, -1, -1, -1,
    2, -1, -1, -1, -1, -1, -1, -1,
    4, -1, -1, -1, -1, -1, -1, -1,
   -1, -1, -1, -1, -1, -1, -1, -1,
    3, -1, -1, -1, -1, -1, -1, -1,
   -1, -1, -1, -1, -1, -1, -1, -1,
    5};

int openhevc_init_context()
{
    ff_hevc_dsp_init(&hevcDsp, 8);
    ff_hevc_pred_init(&hevcPred, 8);

    return 0;
}

void put_hevc_qpel_orcc(i16 _dst[2][64*64], u8 listIdx, u8 _src[71*71], u8 srcstride, u8 _width, u8 _height, i32 mx, i32 my)
{
    u8  *src = &_src[3+3*srcstride];
    i16 *dst = _dst[listIdx];
    u8 width = _width + 1;
    u8 height = _height + 1;
    int idx = LUT_PEL_FUNC[width];

    hevcDsp.put_hevc_qpel[idx][!!my][!!mx](dst, width, src, srcstride, height, mx, my, width);
}

void put_hevc_epel_orcc(i16 _dst[2][64*64], u8 listIdx, u8 _src[71*71], u8 srcstride, u8 _width, u8 _height, i32 mx, i32 my)
{
    u8  *src = &_src[1+1*srcstride];
    i16 *dst = _dst[listIdx];
    u8 width = _width + 1;
    u8 height = _height + 1;
    int idx = LUT_PEL_FUNC[width];

    hevcDsp.put_hevc_epel[idx][!!my][!!mx](dst, width, src, srcstride, height, mx, my, width);
}

/* WEIGHTED PREDICTION */

void put_unweighted_pred_orcc(i16 _src[2][64*64], int _width, int _height, u8 rdList, u8 _dst[64*64])
{
    i16 * src = _src[rdList];
    u8 * dst = _dst;
    u8 width = _width + 1;
    u8 height = _height + 1;
    int idx = LUT_WEIGHTED_FUNC[width];

    hevcDsp.put_unweighted_pred[idx](dst, width, src, width, width, height);
}

void put_weighted_pred_avg_orcc (i16 src[2][64*64], int _width, int _height, u8 dst[64*64])
{
    u8 width = _width + 1;
    u8 height = _height + 1;
    int idx = LUT_WEIGHTED_FUNC[width];

    hevcDsp.put_weighted_pred_avg[idx](dst, width, src[0], src[1], width, width, height);
}

void weighted_pred_orcc(int logWD, int weightCu[2], int offsetCu[2], i16 _src[2][64*64], int _width, int _height, u8 rdList, u8 _dst[64*64])
{
    i16 * src = _src[rdList];
    u8 * dst = _dst;
    u8 width = _width + 1;
    u8 height = _height + 1;
    int wX = weightCu[rdList];
    int oX = offsetCu[rdList];
    int locLogWD = logWD - 14 + 8;
    int idx = LUT_WEIGHTED_FUNC[width];

    hevcDsp.weighted_pred[idx](locLogWD, wX, oX, dst, width, src, width, width, height);
}

void weighted_pred_avg_orcc(int logWD , int weightCu[2], int offsetCu[2], i16 _src[2][64*64], int _width, int _height, u8 _dst[64*64])
{
    i16 * src = _src[0];
    i16 * src1 = _src[1];
    u8 * dst = _dst;
    u8 width = _width + 1;
    u8 height = _height + 1;
    int w0 = weightCu[0];
    int w1 = weightCu[1];
    int o0 = offsetCu[0];
    int o1 = offsetCu[1];
    int locLogWD = logWD - 14 + 8;
    int idx = LUT_WEIGHTED_FUNC[width];

    hevcDsp.weighted_pred_avg[idx](locLogWD, w0, w1, o0, o1, dst, width, src, src1, width, width, height);
}


void pred_planar_orcc(u8 _src[4096], u8 _top[129], u8 _left[129], i32 stride, i32 log2size)
{
    u8 *src        = _src;
    u8 *top  = _top + 1;
    u8 *left = _left + 1;

    hevcPred.pred_planar[log2size - 2](src, top, left, stride);
}

/* SAO */

#define min(a, b) (((a) < (b)) ? (a) : (b))

void saoFilterEdge_orcc(u8 saoEoClass, u8 cIdx, u8 cIdxOffset, u16 idxOrig[2], u8 lcuSizeMax,
	u16 picSize[2], u8 lcuIsPictBorder, i32 saoOffset[5],
	u8 filtAcrossSlcAndTiles,
	u8 * pucOrigPict,
	u8 * pucFiltPict,
	u8 saoTypeIdx[8])
{
	u8 * ptrDst = &pucFiltPict[cIdxOffset * 2048 * 4096 + idxOrig[1] * 4096 + idxOrig[0]];
	u8 * ptrSrc = &pucOrigPict[cIdxOffset * 2048 * 4096 + idxOrig[1] * 4096 + idxOrig[0]];

	int borders[4];
	struct SAOParams sao;
	int i, x, y;
	i16 xMax;
	u16 yMax;
	i16 xMax2;
	u16 yMax2;

	sao.eo_class[cIdx] = saoEoClass;
	for(i = 0; i < 5; i++) {
		sao.offset_val[cIdx][i] = saoOffset[i];
	}

	xMax = min(lcuSizeMax - 1, picSize[0] - idxOrig[0] - 1);
	yMax = min(lcuSizeMax - 1, picSize[1] - idxOrig[1] - 1);
	xMax2 = min(lcuSizeMax, picSize[0] - idxOrig[0]);
	yMax2 = min(lcuSizeMax, picSize[1] - idxOrig[1]);

	borders[0] = (idxOrig[0] == 0);
	borders[1] = (idxOrig[1] == 0);
	borders[2] = (idxOrig[0] + xMax2 == picSize[0]);
	borders[3] = (idxOrig[1] + yMax2 == picSize[1]);

	ff_hevc_sao_edge_filter_0_8_mult_stride_sse(ptrDst, 4096, ptrSrc,
		4096, &sao, borders, xMax2, yMax2, cIdx, NULL, NULL, NULL);
}


void saoBandFilter_orcc(u8 saoLeftClass, i32 saoOffset[5], u8 cIdx, u8 cIdxOffset, i16 idxMin[2],
	i16 idxMax[2],
	u8 * pucOrigPict,
	u8 * pucFiltPict) {

	u8 * ptrDst = &pucFiltPict[cIdxOffset * 2048 * 4096 + idxMin[1] * 4096 + idxMin[0]];
	u8 * ptrSrc = &pucOrigPict[cIdxOffset * 2048 * 4096 + idxMin[1] * 4096 + idxMin[0]];

	struct SAOParams sao;
	int i;

	sao.band_position[cIdx] = saoLeftClass;
	for(i = 0; i < 5; i++) {
		sao.offset_val[cIdx][i] = saoOffset[i];
	}

	ff_hevc_sao_band_filter_0_8_mult_stride_sse(ptrDst, 4096,
		ptrSrc, 4096, &sao, NULL, idxMax[0] - idxMin[0] + 1,
		idxMax[1] - idxMin[1] + 1, cIdx);
}

/* DBF */

void hevc_v_loop_filter_luma_orcc (u8 *_pix, int _offset, int _stride, int *_beta, int *_tc, u8 *_no_p, u8 *_no_q, u16 blkAddr0[2], int idxBlk)
{
  u8 * pix = &_pix[_offset];
  hevcDsp.hevc_v_loop_filter_luma(pix, _stride, _beta, _tc, _no_p, _no_q);
}

void hevc_h_loop_filter_luma_orcc (u8 *_pix, int _offset, int _stride, int *_beta, int *_tc, u8 *_no_p, u8 *_no_q, u16 blkAddr0[2], int idxBlk)
{
  u8 * pix = &_pix[_offset];
  hevcDsp.hevc_h_loop_filter_luma(pix, _stride, _beta, _tc, _no_p, _no_q);
}

void hevc_v_loop_filter_chroma_orcc (u8 *_pix, int _offset, int _stride, int *_tc, u8 *_no_p, u8 *_no_q, u16 blkAddr0[2])
{
  u8 * pix = &_pix[_offset];
  hevcDsp.hevc_v_loop_filter_chroma(pix, _stride, _tc, _no_p, _no_q);
}

void hevc_h_loop_filter_chroma_orcc (u8 *_pix, int _offset, int _stride, int *_tc, u8 *_no_p, u8 *_no_q, u16 blkAddr0[2])
{
  u8 * pix = &_pix[_offset];
  hevcDsp.hevc_h_loop_filter_chroma(pix, _stride, _tc, _no_p, _no_q);
}

