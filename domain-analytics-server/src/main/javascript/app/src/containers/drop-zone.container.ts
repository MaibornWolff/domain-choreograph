import { connect } from 'react-redux';
import { push } from 'connected-react-router';
import { DropZone, DropZoneProps } from '~components/drop-zone/drop-zone';
import { ActionCreators } from '~ducks';

export const DropZoneContainer = connect<{}, DropZoneProps>(
  null,
  dispatch => ({
    onLoad: graph => {
      dispatch(ActionCreators.loadGraph({ graph }));
      dispatch(push('/'));
    }
  }),
)(DropZone);
