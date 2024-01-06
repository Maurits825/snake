package com.snake;

import lombok.Getter;

@Getter
public enum SnakeGridTheme
{
	ORIGINAL(32693, -1, -1),
	TOA(-1, 45510, 45432);

	private final int wallModelId;
	private final int tileModelId1;
	private final int tileModelId2;

	SnakeGridTheme(int wallModelId, int tileModelId1, int tileModelId2)
	{
		this.wallModelId = wallModelId;
		this.tileModelId1 = tileModelId1;
		this.tileModelId2 = tileModelId2;
	}
}
