<template>
	<view class="page">
		<u-icon name="checkmark-circle-fill" color="#2B6CFF" size="64"></u-icon>
		<text class="title">品达物流 · 司机端</text>
		<text class="desc">uview-plus + Pinia 已就绪</text>
		<u-button type="primary" text="主要按钮" customStyle="margin-top: 24px"></u-button>
		<u-button type="primary" plain text="次要按钮" customStyle="margin-top: 16px"></u-button>
	</view>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 120rpx 48rpx;
	box-sizing: border-box;
}

.title {
	margin-top: 32rpx;
	font-size: 40rpx;
	font-weight: 600;
	color: #1a1a1a;
}

.desc {
	margin-top: 12rpx;
	font-size: 28rpx;
	color: #909399;
}
</style>
