import { GlobalState } from '~ducks';
import { selectSelectedNode, selectTheme } from '~selectors/selectors';
import { THEME } from '~styles/theme';
import { graphMock } from '~types/cheoreo-graph.mock';

const mockState: GlobalState = {
  app: {
    graph: graphMock,
    theme: 'light'
  },
};

describe('selectSelectedNode', () => {
  it('should work', () => {
    expect(selectSelectedNode(mockState)).toBe(null);
    expect(selectSelectedNode({
      app: {
        ...mockState.app,
        selectedNodeId: '0'
      }
    })).toBe(graphMock.nodes['0']);
  });
});

describe('selectTheme', () => {
  it('should work', () => {
    expect(selectTheme(mockState)).toBe(THEME);
  });
});
