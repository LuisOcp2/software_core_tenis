package raven.cell;

public interface TableActionDevolucionEvent {
    void onView(int row);

    void onAuthorize(int row);

    void onReject(int row);

    void onAnulate(int row);
}
